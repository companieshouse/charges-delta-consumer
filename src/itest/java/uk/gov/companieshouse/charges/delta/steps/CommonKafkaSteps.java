package uk.gov.companieshouse.charges.delta.steps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.delta.ChsDelta;

public class CommonKafkaSteps {

    private ChsDelta lastMessageSent;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;


}
