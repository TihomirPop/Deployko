package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.CiPipelineCompletedEventDomainService;
import hr.tvz.popovic.deployko.application.port.in.DeployServiceUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindLastCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.RecordCiDeploymentPort;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableRabbit
@EnableConfigurationProperties(CiEventsProperties.class)
public class CiEventsConfiguration {

    @Bean
    TopicExchange ciEventsExchange(CiEventsProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    Queue ciPipelineCompletedQueue(CiEventsProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Binding ciPipelineCompletedBinding(Queue ciPipelineCompletedQueue, TopicExchange ciEventsExchange, CiEventsProperties properties) {
        return BindingBuilder
                .bind(ciPipelineCompletedQueue)
                .to(ciEventsExchange)
                .with(properties.routingKey());
    }

    @Bean
    RabbitListenerContainerFactory<SimpleMessageListenerContainer> ciEventsRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            CiEventsProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(properties.batchSize());
        factory.setReceiveTimeout(properties.receiveTimeoutMillis());
        factory.setAutoStartup(properties.listenerAutoStartup());
        return factory;
    }

    @Bean
    CiPipelineCompletedEventDomainService ciPipelineCompletedEventDomainService(
            FindLastCiDeploymentPort findLastCiDeploymentPort,
            RecordCiDeploymentPort recordCiDeploymentPort,
            DeployServiceUseCase deployServiceUseCase,
            Clock clock,
            CiEventsProperties properties
    ) {
        return new CiPipelineCompletedEventDomainService(
                findLastCiDeploymentPort,
                recordCiDeploymentPort,
                deployServiceUseCase,
                clock,
                properties.deploymentThrottleWindow()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }
}
