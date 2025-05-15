package ca.bc.gov.educ.challenge.reports.api.properties;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Class holds all application properties
 *
 */
@Component
@Getter
@Setter
public class ApplicationProperties {
  public static final Executor bgTask = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("bg-task-executor-%d").build())
    .setCorePoolSize(1).setMaximumPoolSize(1).setKeepAliveTime(Duration.ofSeconds(60)).build();
  public static final String CHALLENGE_REPORTS_API = "CHALLENGE_REPORTS_API";
  public static final String CORRELATION_ID = "correlationID";
  /**
   * The Client id.
   */
  @Value("${client.id}")
  private String clientID;
  /**
   * The Client secret.
   */
  @Value("${client.secret}")
  private String clientSecret;
  /**
   * The Token url.
   */
  @Value("${url.token}")
  private String tokenURL;

  @Value("${nats.server}")
  private String server;

  @Value("${ches.endpoint.url}")
  private String chesEndpointURL;
  @Value("${ches.client.id}")
  private String chesClientID;
  @Value("${ches.client.secret}")
  private String chesClientSecret;
  @Value("${ches.token.url}")
  private String chesTokenURL;

  @Value("${nats.maxReconnect}")
  private int maxReconnect;

  @Value("${nats.connectionName}")
  private String connectionName;

  @Value("${threads.min.subscriber}")
  private Integer minSubscriberThreads;
  @Value("${threads.max.subscriber}")
  private Integer maxSubscriberThreads;
  @Value("${url.api.institute}")
  private String instituteApiURL;
  @Value("${url.api.coreg}")
  private String coregApiURL;
  @Value("${url.api.sdc}")
  private String sdcApiURL;
  @Value("${url.api.gradstudent}")
  private String gradStudentApiURL;
}
