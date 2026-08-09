package com.codestorykh.screening.config;

import com.codestorykh.screening.dto.event.AccountLinkedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "screening.kafka.enabled", havingValue = "true")
public class ScreeningKafkaConfig {

    @Bean
    public ConsumerFactory<String, AccountLinkedEvent> accountLinkedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${screening.kafka.group-id:screening-account-link-group}") String groupId
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);

        // Dedicated camelCase mapper — must match account Kafka producer
        JsonMapper kafkaMapper = JsonMapper.builder().build();
        JacksonJsonDeserializer<AccountLinkedEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(AccountLinkedEvent.class, kafkaMapper)
                        .ignoreTypeHeaders()
                        .trustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AccountLinkedEvent> accountLinkedListenerContainerFactory(
            ConsumerFactory<String, AccountLinkedEvent> accountLinkedConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, AccountLinkedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(accountLinkedConsumerFactory);
        return factory;
    }
}
