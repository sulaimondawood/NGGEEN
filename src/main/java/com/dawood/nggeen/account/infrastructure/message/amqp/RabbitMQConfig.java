package com.dawood.nggeen.account.infrastructure.message.amqp;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private final String EXCHANGE = "nggeen.exchange";

    public static final String DLX_EXCHANGE = "nggeen.dlx";
    public static final String DEAD_LETTER_QUEUE = "nggeen.dead-letter.queue";

    public static final String EMAIL_VERIFICATION = "email.verification";
    public static final String EMAIL_VERIFICATION_QUEUE = "email.verification.queue";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue dLXQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public TopicExchange dlxTopicExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue emailVerificationQueue() {
        return QueueBuilder.durable(EMAIL_VERIFICATION_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Binding emailVerificationBinding(Queue emailVerificationQueue, TopicExchange topicExchange) {
        return BindingBuilder
                .bind(emailVerificationQueue)
                .to(topicExchange)
                .with(EMAIL_VERIFICATION);
    }


}
