package uk.gov.companieshouse.charges.delta.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.charges.delta.processor.ChargesDeltaProcessor;
import uk.gov.companieshouse.delta.ChsDelta;


@Component
public class ChargesDeltaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChargesDeltaConsumer.class);

    private final ChargesDeltaProcessor deltaProcessor;

    @Autowired
    public ChargesDeltaConsumer(ChargesDeltaProcessor deltaProcessor) {
        this.deltaProcessor = deltaProcessor;
    }

    /**
     * Receives Main topic messages.
     */
    @KafkaListener(id = "${charges.delta.main-id}",
            topics = "${charges.delta.topic.main}",
            groupId = "${charges.delta.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receiveMainMessages(Message<ChsDelta> chsDeltaMessage) {
        LOGGER.info("A new message read from MAIN topic with payload: "
                + chsDeltaMessage.getPayload());
        //deltaProcessor.processDelta(chsDeltaMessage);
    }

    /**
     * Receives Retry topic messages.
     */
    @KafkaListener(id = "${charges.delta.retry-id}",
            topics = "${charges.delta.topic.retry}",
            groupId = "${charges.delta.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receiveRetryMessages(Message<ChsDelta> message) {
        LOGGER.info(String.format("A new message read from RETRY topic with payload:%s "
                + "and headers:%s ", message.getPayload(), message.getHeaders()));
        //deltaProcessor.processDelta(message);
    }

}
