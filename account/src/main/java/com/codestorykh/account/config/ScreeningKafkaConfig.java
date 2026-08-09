package com.codestorykh.account.config;

import com.codestorykh.account.client.screening.AccountLinkedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "screening.kafka.enabled", havingValue = "true")
public class ScreeningKafkaConfig {

    @Bean
    public ProducerFactory<String, AccountLinkedEvent> screeningAccountLinkedProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        // Dedicated camelCase mapper — do not reuse HTTP SNAKE_CASE JsonMapper bean
        JsonMapper kafkaMapper = JsonMapper.builder().build();
        JacksonJsonSerializer<AccountLinkedEvent> valueSerializer = new JacksonJsonSerializer<>(kafkaMapper);
        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, AccountLinkedEvent> screeningAccountLinkedKafkaTemplate(
            ProducerFactory<String, AccountLinkedEvent> screeningAccountLinkedProducerFactory
    ) {
        return new KafkaTemplate<>(screeningAccountLinkedProducerFactory);
    }
}
