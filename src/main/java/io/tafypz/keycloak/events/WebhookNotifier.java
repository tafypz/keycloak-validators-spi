package io.tafypz.keycloak.events;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.jboss.logging.Logger;

class WebhookNotifier implements Notifier {

  private static final Logger log = Logger.getLogger(WebhookNotifier.class);

  private final String url;
  private final HttpClient httpClient;

  WebhookNotifier(String url) {
    this.url = url;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @Override
  public void send(String jsonPayload) throws Exception {
    log.debugf("WebhookNotifier: sending notification to %s with %s", url, jsonPayload);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

    HttpResponse<Void> response =
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    int status = response.statusCode();
    if (status < 200 || status >= 300) {
      log.warnf(
          "EmailUpdateNotifier: webhook returned non-2xx status %d for URL %s", status, url);
    } else {
      log.debugf(
          "EmailUpdateNotifier: webhook notification sent successfully to %s (status %d)",
          url, status);
    }
  }
}
