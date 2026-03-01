package io.tafypz.keycloak.events;

import java.time.Instant;
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

    sendSafely(buildPayload(userId, user.getEmail(), previousEmail, realm.getId(), "user"));
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
    sendSafely(buildPayload(userId, user.getEmail(), null, realm.getId(), "admin"));
  }

  private void sendSafely(String payload) {
    try {
      factory.getNotifier().send(payload);
    } catch (Exception e) {
      log.errorf(e, "EmailUpdateNotifier: failed to send notification: %s", e.getMessage());
    }
  }

  private String buildPayload(
      String userId, String email, String previousEmail, String realmId, String source) {
    try {
      var node = JsonSerialization.createObjectNode();
      node.put("event", "email.verified");
      node.put("userId", userId);
      node.put("email", email);
      if (previousEmail != null) node.put("previousEmail", previousEmail);
      node.put("realmId", realmId);
      node.put("source", source);
      node.put("timestamp", Instant.now().toString());
      return JsonSerialization.writeValueAsString(node);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize notification payload", e);
    }
  }

  @Override
  public void close() {
    // no resources to release
  }
}
