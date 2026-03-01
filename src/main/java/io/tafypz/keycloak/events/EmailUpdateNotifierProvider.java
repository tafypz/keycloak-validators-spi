package io.tafypz.keycloak.events;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;

class EmailUpdateNotifierProvider implements EventListenerProvider {

  private static final Logger log = Logger.getLogger(EmailUpdateNotifierProvider.class);

  // Maps Keycloak event detail key → JSON payload field name.
  // Only fields present here are watched; any other profile change is ignored.
  // Note: Keycloak uses the user profile attribute name as the detail key (e.g. "date-of-birth").
  // Verify against actual UPDATE_PROFILE event details if adding new fields.
  private static final Map<String, String> WATCHED_FIELDS =
      Map.of(
          "first_name", "firstName",
          "last_name", "lastName",
          "date-of-birth", "dateOfBirth",
          "phoneNumber", "phoneNumber");

  private final KeycloakSession session;
  private final EmailUpdateNotifierFactory factory;

  EmailUpdateNotifierProvider(KeycloakSession session, EmailUpdateNotifierFactory factory) {
    this.session = session;
    this.factory = factory;
  }

  @Override
  public void onEvent(Event event) {
    if (factory.getNotifier() == null) return;

    if (EventType.UPDATE_EMAIL.equals(event.getType())) {
      handleUpdateEmail(event);
    } else if (EventType.UPDATE_PROFILE.equals(event.getType())) {
      handleUpdateProfile(event);
    }
  }

  private void handleUpdateEmail(Event event) {
    // In Keycloak 26.x, UPDATE_EMAIL fires after the user has already clicked the verification
    // link — it is the terminal event of the email-update flow, not a precursor to VERIFY_EMAIL.
    String userId = event.getUserId();
    String previousEmail =
        event.getDetails() != null ? event.getDetails().get("previous_email") : null;

    RealmModel realm = session.realms().getRealm(event.getRealmId());
    if (realm == null) return;
    UserModel user = session.users().getUserById(realm, userId);
    if (user == null) return;

    sendSafely(buildEmailPayload(userId, user.getEmail(), previousEmail, realm.getId(), "user"));
  }

  private void handleUpdateProfile(Event event) {
    Map<String, String> details = event.getDetails();
    if (details == null) return;

    // Build a changes map containing only the watched fields that actually changed.
    Map<String, ProfileUpdatedPayload.FieldChange> changes = new LinkedHashMap<>();
    for (Map.Entry<String, String> watched : WATCHED_FIELDS.entrySet()) {
      String detailKey = watched.getKey();
      String payloadKey = watched.getValue();
      String updated = details.get("updated_" + detailKey);
      if (updated != null) {
        changes.put(payloadKey,
            new ProfileUpdatedPayload.FieldChange(details.get("previous_" + detailKey), updated));
      }
    }

    // Skip if none of the watched fields changed (e.g. user updated an unrelated attribute).
    if (changes.isEmpty()) return;

    RealmModel realm = session.realms().getRealm(event.getRealmId());
    if (realm == null) return;

    sendSafely(buildProfilePayload(event.getUserId(), changes, realm.getId(), "user"));
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    if (factory.getNotifier() == null) return;

    if (ResourceType.USER != adminEvent.getResourceType()) return;
    if (OperationType.UPDATE != adminEvent.getOperationType()) return;

    String representation = adminEvent.getRepresentation();
    if (representation == null || !representation.contains("\"emailVerified\":true")) return;

    // Resource path for a user update is "users/<userId>"
    String resourcePath = adminEvent.getResourcePath();
    if (resourcePath == null || !resourcePath.startsWith("users/")) return;
    String userId = resourcePath.substring("users/".length());
    if (userId.isEmpty() || userId.contains("/")) return;

    RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
    if (realm == null) return;
    UserModel user = session.users().getUserById(realm, userId);
    if (user == null) return;

    // No previousEmail available for admin-initiated changes
    sendSafely(buildEmailPayload(userId, user.getEmail(), null, realm.getId(), "admin"));
  }

  private void sendSafely(String payload) {
    try {
      factory.getNotifier().send(payload);
    } catch (Exception e) {
      log.errorf(e, "EmailUpdateNotifier: failed to send notification: %s", e.getMessage());
    }
  }

  private String buildEmailPayload(
      String userId, String email, String previousEmail, String realmId, String source) {
    try {
      return JsonSerialization.writeValueAsString(
          new EmailUpdatedPayload(
              "email.verified", userId, email, previousEmail, realmId, source,
              Instant.now().toString()));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize notification payload", e);
    }
  }

  private String buildProfilePayload(
      String userId, Map<String, ProfileUpdatedPayload.FieldChange> changes,
      String realmId, String source) {
    try {
      return JsonSerialization.writeValueAsString(
          new ProfileUpdatedPayload(
              "profile.updated", userId, realmId, source, Instant.now().toString(), changes));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize notification payload", e);
    }
  }

  @Override
  public void close() {
    // no resources to release
  }
}
