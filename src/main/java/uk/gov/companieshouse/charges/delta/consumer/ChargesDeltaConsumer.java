package uk.gov.companieshouse.charges.delta.consumer;

import static java.lang.String.format;
import static java.time.Duration.between;

import java.time.Instant;

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
        Instant startTime = Instant.now();
        ChsDelta chsDelta = message.getPayload();
        String contextId = chsDelta.getContextId();
        logger.info(format("A new message successfully picked up from topic: %s, "
                        + "partition: %s and offset: %s with contextId: %s",
                topic, partition, offset, contextId));

        try {
            if (Boolean.TRUE.equals(message.getPayload().getIsDelete())) {
                deltaProcessor.processDelete(message);
                logger.info(format("Charges Delete message with contextId: %s is successfully "
                                + "processed in %d milliseconds", contextId,
                        between(startTime, Instant.now()).toMillis()));
            } else {
                deltaProcessor.processDelta(message);
                logger.info(format("Charges Delta message with contextId: %s is successfully "
                                + "processed in %d milliseconds", contextId,
                        between(startTime, Instant.now()).toMillis()));
            }
        } catch (Exception exception) {
            logger.errorContext(contextId, format("Exception occurred while processing "
                    + "message on the topic: %s", topic), exception, null);
            throw exception;
        }
    }

}
