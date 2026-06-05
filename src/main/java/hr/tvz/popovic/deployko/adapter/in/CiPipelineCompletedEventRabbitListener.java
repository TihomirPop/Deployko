package hr.tvz.popovic.deployko.adapter.in;

import com.rabbitmq.client.Channel;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.HandleCiPipelineCompletedEventUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public final class CiPipelineCompletedEventRabbitListener {

    private static final Logger log = LoggerFactory.getLogger(CiPipelineCompletedEventRabbitListener.class);

    private static final String SUPPORTED_EVENT = "pipeline_completed";
    private static final String SUPPORTED_STATUS = "success";

    private final ObjectMapper objectMapper;
    private final HandleCiPipelineCompletedEventUseCase useCase;

    public CiPipelineCompletedEventRabbitListener(
            ObjectMapper objectMapper,
            HandleCiPipelineCompletedEventUseCase useCase
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
    }

    @RabbitListener(queues = "#{ciPipelineCompletedQueue.name}", containerFactory = "ciEventsRabbitListenerContainerFactory")
    public void consume(List<Message> messages, Channel channel) throws IOException {
        List<ParsedCiEvent> validEvents = new ArrayList<>();
        Map<ServiceName, ParsedCiEvent> newestEventsByService = new HashMap<>();

        for (Message message : messages) {
            switch (parse(message)) {
                case ParsedMessage.Valid valid -> {
                    validEvents.add(valid.event());
                    newestEventsByService.merge(valid.event().serviceName(), valid.event(), this::newestByBuildNumber);
                }
                case ParsedMessage.Ignored _ -> acknowledge(channel, message);
            }
        }

        for (ParsedCiEvent event : validEvents) {
            if (newestEventsByService.get(event.serviceName()) != event) {
                acknowledge(channel, event.message());
            }
        }

        for (ParsedCiEvent event : newestEventsByService.values()) {
            switch (useCase.handleCiPipelineCompletedEvent(
                    new HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventCommand(
                            event.serviceName(),
                            event.imageVersion(),
                            event.buildNumber()
                    )
            )) {
                case HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.Deployed _,
                     HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.SkippedRecentDeployment _,
                     HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.ServiceNotFound _ ->
                        acknowledge(channel, event.message());
                case HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.DeploymentFailure _,
                     HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.LastDeploymentLookupFailure _,
                     HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.RecordDeploymentFailure _ ->
                        rejectForRetry(channel, event.message());
            }
        }
    }

    private ParsedMessage parse(Message message) {
        try {
            CiPipelineCompletedMessage body = objectMapper.readValue(message.getBody(), CiPipelineCompletedMessage.class);
            if (!SUPPORTED_EVENT.equals(body.event()) || !SUPPORTED_STATUS.equals(body.status())) {
                return new ParsedMessage.Ignored();
            }
            Long buildNumber = Objects.requireNonNull(body.buildNumber(), "buildNumber must not be null");
            if (buildNumber < 1) {
                throw new IllegalArgumentException("buildNumber must be positive");
            }

            return new ParsedMessage.Valid(new ParsedCiEvent(
                    message,
                    new ServiceName(body.service()),
                    new ImageVersion(body.tag()),
                    buildNumber
            ));
        } catch (RuntimeException exception) {
            log.warn(
                    "ignoring invalid CI pipeline completed event payload: {} ({})",
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    exception.getMessage()
            );
            return new ParsedMessage.Ignored();
        }
    }

    private ParsedCiEvent newestByBuildNumber(ParsedCiEvent first, ParsedCiEvent second) {
        if (second.buildNumber() >= first.buildNumber()) {
            return second;
        }
        return first;
    }

    private static void acknowledge(Channel channel, Message message) throws IOException {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    private static void rejectForRetry(Channel channel, Message message) throws IOException {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
    }

    private sealed interface ParsedMessage permits ParsedMessage.Valid, ParsedMessage.Ignored {

        record Valid(ParsedCiEvent event) implements ParsedMessage {
        }

        record Ignored() implements ParsedMessage {
        }
    }

    private static final class ParsedCiEvent {

        private final Message message;
        private final ServiceName serviceName;
        private final ImageVersion imageVersion;
        private final long buildNumber;

        private ParsedCiEvent(Message message, ServiceName serviceName, ImageVersion imageVersion, long buildNumber) {
            this.message = message;
            this.serviceName = serviceName;
            this.imageVersion = imageVersion;
            this.buildNumber = buildNumber;
        }

        private Message message() {
            return message;
        }

        private ServiceName serviceName() {
            return serviceName;
        }

        private ImageVersion imageVersion() {
            return imageVersion;
        }

        private long buildNumber() {
            return buildNumber;
        }
    }

    private record CiPipelineCompletedMessage(
            String event,
            String status,
            String service,
            String tag,
            String repo,
            Long buildNumber
    ) {
    }
}
