package uk.gov.companieshouse.charges.delta.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.producer.ChargesDeltaProducer;
import uk.gov.companieshouse.charges.delta.transformer.ChargesApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;


@Component
public class ChargesDeltaProcessor {

    private final ChargesDeltaProducer deltaProducer;
    private final ChargesApiTransformer transformer;
    private final Logger logger;

    /**
     * The constructor.
     */
    @Autowired
    public ChargesDeltaProcessor(ChargesDeltaProducer deltaProducer,
                                 ChargesApiTransformer transformer,
                                 Logger logger) {
        this.deltaProducer = deltaProducer;
        this.transformer = transformer;
        this.logger = logger;

    }

    /**
     * Process CHS Delta message.
     */
    public void processDelta(Message<ChsDelta> chsDelta) {
        try {
            MessageHeaders headers = chsDelta.getHeaders();
            final ChsDelta payload = chsDelta.getPayload();
            final String receivedTopic =
                    Objects.requireNonNull(headers.get(KafkaHeaders.RECEIVED_TOPIC)).toString();

            ObjectMapper mapper = new ObjectMapper();
            ChargesDelta chargesDelta = mapper.readValue(payload.getData(), ChargesDelta.class);
            logger.trace(String.format("DSND-514: ChargesDelta extracted "
                    + "from a Kafka message: %s", chargesDelta));
            if (chargesDelta.getCharges().size() > 0) {
                // assuming we always get only one charge item inside charges delta
                transformer.transform(chargesDelta.getCharges().get(0));
            } else {
                throw new NonRetryableErrorException("No charge item found inside ChargesDelta");
            }
        } catch (RetryableErrorException ex) {
            retryDeltaMessage(chsDelta);
        } catch (Exception ex) {
            handleErrorMessage(chsDelta);
            // send to error topic
        }
    }

    public void retryDeltaMessage(Message<ChsDelta> chsDelta) {

    }

    private void handleErrorMessage(Message<ChsDelta> chsDelta) {

    }

}
