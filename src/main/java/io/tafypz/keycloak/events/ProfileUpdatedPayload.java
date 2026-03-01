package io.tafypz.keycloak.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
record ProfileUpdatedPayload(
    String event,
    String userId,
    String realmId,
    String source,
    String timestamp,
    Map<String, FieldChange> changes) {

  record FieldChange(String previous, String current) {}
}
