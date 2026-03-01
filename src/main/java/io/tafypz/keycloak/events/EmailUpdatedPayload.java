package io.tafypz.keycloak.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
record EmailUpdatedPayload(
    String event,
    String userId,
    String email,
    String previousEmail,
    String realmId,
    String source,
    String timestamp) {}
