package io.tafypz.keycloak.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
record UserRegisteredPayload(
    String event,
    String userId,
    String email,
    boolean emailVerified,
    String realmId,
    String source,
    String timestamp,
    Profile profile) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Profile(String firstName, String lastName, String phoneNumber, String dateOfBirth) {}
}
