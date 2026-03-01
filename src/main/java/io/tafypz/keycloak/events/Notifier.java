package io.tafypz.keycloak.events;

interface Notifier {
  void send(String jsonPayload) throws Exception;
}
