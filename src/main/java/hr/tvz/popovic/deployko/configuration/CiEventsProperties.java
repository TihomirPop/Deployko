package hr.tvz.popovic.deployko.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

@ConfigurationProperties(prefix = "deployko.ci-events")
public record CiEventsProperties(
        String exchange,
        String queue,
        String routingKey,
        Integer batchSize,
        Long receiveTimeoutMillis,
        Duration deploymentThrottleWindow,
        Boolean listenerAutoStartup
) {

    public CiEventsProperties {
        exchange = StringUtils.hasText(exchange) ? exchange : "ci-events";
        queue = StringUtils.hasText(queue) ? queue : "deployko.ci.pipeline.completed";
        routingKey = StringUtils.hasText(routingKey) ? routingKey : "pipeline.completed";
        batchSize = batchSize == null ? 10 : batchSize;
        receiveTimeoutMillis = receiveTimeoutMillis == null ? 1_000L : receiveTimeoutMillis;
        deploymentThrottleWindow = deploymentThrottleWindow == null ? Duration.ofMinutes(5) : deploymentThrottleWindow;
        listenerAutoStartup = listenerAutoStartup == null ? false : listenerAutoStartup;

        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (receiveTimeoutMillis < 1) {
            throw new IllegalArgumentException("receiveTimeoutMillis must be positive");
        }
        if (deploymentThrottleWindow.isNegative() || deploymentThrottleWindow.isZero()) {
            throw new IllegalArgumentException("deploymentThrottleWindow must be positive");
        }
    }
}
