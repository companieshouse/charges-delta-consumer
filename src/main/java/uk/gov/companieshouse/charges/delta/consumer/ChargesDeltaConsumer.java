package uk.gov.companieshouse.charges.delta.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
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
    @RetryableTopic(attempts = "${charges.delta.retry-attempts}",
            backoff = @Backoff(delayExpression = "${charges.delta.backoff-delay}"),
            fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${charges.delta.group-id}-retry",
            dltTopicSuffix = "-${charges.delta.group-id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class)
    @KafkaListener(topics = "${charges.delta.topic}",
            groupId = "${charges.delta.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receiveMainMessages(Message<ChsDelta> message,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                                    @Header(KafkaHeaders.OFFSET) String offset) {
        ChsDelta chsDelta = message.getPayload();
        logger.info(String.format("A new message successfully picked up from topic: %s, "
                        + "partition: %s and offset: %s with contextId: %s",
                topic, partition, offset, chsDelta.getContextId()));

        try {
            if (Boolean.TRUE.equals(message.getPayload().getIsDelete())) {
                deltaProcessor.processDelete(message);
            } else {
                deltaProcessor.processDelta(message);
            }
        } catch (Exception exception) {
            logger.error(String.format("Exception occurred while processing the topic: %s "
                    + "with contextId: %s", topic, chsDelta.getContextId()), exception);
            throw exception;
        }
    }

}
