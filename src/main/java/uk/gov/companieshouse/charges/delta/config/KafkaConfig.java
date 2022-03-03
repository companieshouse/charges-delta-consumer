package uk.gov.companieshouse.charges.delta.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import uk.gov.companieshouse.charges.delta.serialization.ChsDeltaDeserializer;
import uk.gov.companieshouse.delta.ChsDelta;

@Configuration
@Profile("!test")
public class KafkaConfig {

    private final ChsDeltaDeserializer chsDeltaDeserializer;
    private final String bootstrapServers;

    public KafkaConfig(ChsDeltaDeserializer chsDeltaDeserializer, @Value("${spring.kafka"
            + ".bootstrap-servers}") String bootstrapServers) {
        this.chsDeltaDeserializer = chsDeltaDeserializer;
        this.bootstrapServers = bootstrapServers;
    }

    /**
     * Kafka Consumer Factory Message.
     */
    @Bean
    public ConsumerFactory<String, ChsDelta> kafkaConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                chsDeltaDeserializer);
    }

    /**
     * Kafka Listener Container Factory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChsDelta> listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChsDelta> factory
                = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory());
        return factory;
    }

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ChsDeltaDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

}


