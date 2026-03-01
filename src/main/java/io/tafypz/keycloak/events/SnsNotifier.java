package io.tafypz.keycloak.events;

import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsException;

class SnsNotifier implements Notifier {

  private static final Logger log = Logger.getLogger(SnsNotifier.class);

  private final String topicArn;
  private final String region;
  private volatile SnsClient client;

  SnsNotifier(String topicArn, String region) {
    this.topicArn = topicArn;
    this.region = region;
  }

  private SnsClient getClient() {
    if (client == null) {
      synchronized (this) {
        if (client == null) {
          client = SnsClient.builder().region(Region.of(region)).build();
        }
      }
    }
    return client;
  }

  @Override
  public void send(String jsonPayload) throws Exception {
    try {
      getClient().publish(r -> r.topicArn(topicArn).message(jsonPayload));
      log.debugf("EmailUpdateNotifier: SNS message published to %s", topicArn);
    } catch (SnsException e) {
      log.errorf(e, "EmailUpdateNotifier: failed to publish SNS message: %s", e.getMessage());
      throw e;
    }
  }
}
