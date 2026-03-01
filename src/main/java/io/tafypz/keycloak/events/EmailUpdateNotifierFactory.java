package io.tafypz.keycloak.events;

import com.google.auto.service.AutoService;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(EventListenerProviderFactory.class)
public class EmailUpdateNotifierFactory implements EventListenerProviderFactory {

  private static final Logger log = Logger.getLogger(EmailUpdateNotifierFactory.class);

  static final String PROVIDER_ID = "email-update-notifier";

  private Notifier notifier;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new EmailUpdateNotifierProvider(session, this);
  }

  @Override
  public void init(Config.Scope config) {
    String type = config.get("notification-type");
    if (type == null) {
      log.warn(
          "EmailUpdateNotifier: 'notification-type' is not configured; notifications disabled.");
      return;
    }
    switch (type) {
      case "sns":
        {
          String arn = config.get("sns-topic-arn");
          String region = config.get("sns-region", "us-east-1");
          if (arn == null) {
            log.warn(
                "EmailUpdateNotifier: 'sns-topic-arn' is required for type=sns;"
                    + " notifications disabled.");
            return;
          }
          notifier = new SnsNotifier(arn, region);
          log.infof(
              "EmailUpdateNotifier initialized with SNS topic %s in region %s", arn, region);
          break;
        }
      case "webhook":
        {
          String url = config.get("webhook-url");
          if (url == null) {
            log.warn(
                "EmailUpdateNotifier: 'webhook-url' is required for type=webhook;"
                    + " notifications disabled.");
            return;
          }
          notifier = new WebhookNotifier(url);
          log.infof("EmailUpdateNotifier initialized with webhook URL %s", url);
          break;
        }
      default:
        log.warnf(
            "EmailUpdateNotifier: unknown notification-type '%s'; notifications disabled.", type);
    }
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {}

  @Override
  public void close() {}

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  Notifier getNotifier() {
    return notifier;
  }
}
