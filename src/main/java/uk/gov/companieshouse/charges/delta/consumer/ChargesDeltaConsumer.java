package uk.gov.companieshouse.charges.delta.consumer;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.charges.delta.processor.ChargesDeltaProcessor;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;


@Component
public class ChargesDeltaConsumer {

    private final ChargesDeltaProcessor deltaProcessor;
    private final Logger logger;

    @Autowired
    public ChargesDeltaConsumer(ChargesDeltaProcessor deltaProcessor, Logger logger) {
        this.deltaProcessor = deltaProcessor;
        this.logger = logger;
    }

    /**
     * Receives Main topic messages.
     */
    @KafkaListener(id = "${charges.delta.main-id}",
            topics = "${charges.delta.topic.main}",
            groupId = "${charges.delta.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receiveMainMessages(Message<ChsDelta> chsDeltaMessage) {
        logger.info(String.format("DSND-493: New message read from MAIN topic with payload: %s",
                chsDeltaMessage.getPayload()));
    }

    /**
     * Receives Retry topic messages.
     */
    @KafkaListener(id = "${charges.delta.retry-id}",
            topics = "${charges.delta.topic.retry}",
            groupId = "${charges.delta.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receiveRetryMessages(Message<ChsDelta> message) {
        logger.info(String.format("A new message read from RETRY topic with payload:%s "
                + "and headers:%s ", message.getPayload(), message.getHeaders()));

    }

}
