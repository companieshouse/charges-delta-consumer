package uk.gov.companieshouse.charges.delta.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.charges.delta.AbstractIntegrationTest;
import uk.gov.companieshouse.delta.ChsDelta;

public class ChargesDeltaConsumerITest extends AbstractIntegrationTest {

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${charges.delta.topic}")
    private String mainTopic;

    @Test
    public void testSendingKafkaMessage() {
        ChsDelta chsDelta = new ChsDelta("{ \"key\": \"value\" }", 1, "some_id");
        kafkaTemplate.send(mainTopic, chsDelta);
    }

}
