package hr.tvz.popovic.deployko.adapter.in;

import com.rabbitmq.client.Channel;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.port.in.HandleCiPipelineCompletedEventUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CiPipelineCompletedEventRabbitListenerTest {

    @Test
    void consumes_only_highest_build_number_event_per_service() throws IOException {
        StubUseCase useCase = new StubUseCase(
                new HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.Deployed()
        );
        CiPipelineCompletedEventRabbitListener listener = new CiPipelineCompletedEventRabbitListener(
                new ObjectMapper(),
                useCase
        );
        RecordingChannel channel = RecordingChannel.create();
        Message older = message(1, """
                {"event":"pipeline_completed","status":"success","service":"deployko","tag":"42-aaaaaaa","repo":"git","buildNumber":42}
                """);
        Message newer = message(2, """
                {"event":"pipeline_completed","status":"success","service":"deployko","tag":"43-bbbbbbb","repo":"git","buildNumber":43}
                """);

        listener.consume(List.of(older, newer), channel.proxy());

        assertThat(useCase.commands).containsExactly(
                new HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventCommand(
                        new ServiceName("deployko"),
                        new ImageVersion("43-bbbbbbb"),
                        43
                )
        );
        assertThat(channel.acks).containsExactly(new Ack(1, false), new Ack(2, false));
        assertThat(channel.nacks).isEmpty();
    }

    @Test
    void acknowledges_invalid_and_unsupported_events_without_calling_use_case() throws IOException {
        StubUseCase useCase = new StubUseCase(
                new HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.Deployed()
        );
        CiPipelineCompletedEventRabbitListener listener = new CiPipelineCompletedEventRabbitListener(
                new ObjectMapper(),
                useCase
        );
        RecordingChannel channel = RecordingChannel.create();
        Message invalid = message(1, "not-json");
        Message failed = message(2, """
                {"event":"pipeline_completed","status":"failure","service":"deployko","tag":"43-bbbbbbb","repo":"git","buildNumber":43}
                """);

        listener.consume(List.of(invalid, failed), channel.proxy());

        assertThat(useCase.commands).isEmpty();
        assertThat(channel.acks).containsExactly(new Ack(1, false), new Ack(2, false));
        assertThat(channel.nacks).isEmpty();
    }

    @Test
    void rejects_deployment_failures_for_retry() throws IOException {
        StubUseCase useCase = new StubUseCase(
                new HandleCiPipelineCompletedEventUseCase.HandleCiPipelineCompletedEventResult.DeploymentFailure()
        );
        CiPipelineCompletedEventRabbitListener listener = new CiPipelineCompletedEventRabbitListener(
                new ObjectMapper(),
                useCase
        );
        RecordingChannel channel = RecordingChannel.create();
        Message message = message(1, """
                {"event":"pipeline_completed","status":"success","service":"deployko","tag":"43-bbbbbbb","repo":"git","buildNumber":43}
                """);

        listener.consume(List.of(message), channel.proxy());

        assertThat(channel.acks).isEmpty();
        assertThat(channel.nacks).containsExactly(new Nack(1, false, true));
    }

    private static Message message(long deliveryTag, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    private static final class StubUseCase implements HandleCiPipelineCompletedEventUseCase {

        private final HandleCiPipelineCompletedEventResult result;
        private final List<HandleCiPipelineCompletedEventCommand> commands = new ArrayList<>();

        private StubUseCase(HandleCiPipelineCompletedEventResult result) {
            this.result = result;
        }

        @Override
        public HandleCiPipelineCompletedEventResult handleCiPipelineCompletedEvent(
                HandleCiPipelineCompletedEventCommand command
        ) {
            commands.add(command);
            return result;
        }
    }

    private record Ack(long deliveryTag, boolean multiple) {
    }

    private record Nack(long deliveryTag, boolean multiple, boolean requeue) {
    }

    private static final class RecordingChannel implements InvocationHandler {

        private final List<Ack> acks = new ArrayList<>();
        private final List<Nack> nacks = new ArrayList<>();
        private Channel proxy;

        private static RecordingChannel create() {
            RecordingChannel recordingChannel = new RecordingChannel();
            recordingChannel.proxy = (Channel) Proxy.newProxyInstance(
                    Channel.class.getClassLoader(),
                    new Class<?>[]{Channel.class},
                    recordingChannel
            );
            return recordingChannel;
        }

        private Channel proxy() {
            return proxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("basicAck")) {
                acks.add(new Ack((long) args[0], (boolean) args[1]));
                return null;
            }
            if (method.getName().equals("basicNack")) {
                nacks.add(new Nack((long) args[0], (boolean) args[1], (boolean) args[2]));
                return null;
            }
            return defaultValue(method.getReturnType());
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType.equals(boolean.class)) {
                return false;
            }
            if (returnType.equals(int.class)) {
                return 0;
            }
            if (returnType.equals(long.class)) {
                return 0L;
            }
            return null;
        }
    }
}
