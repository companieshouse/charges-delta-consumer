package uk.gov.companieshouse.charges.delta.serialization;

import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.logging.DataMapHolder;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class ChsDeltaDeserializer implements Deserializer<ChsDelta> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Override
    public ChsDelta deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<ChsDelta> reader = new ReflectDatumReader<>(ChsDelta.class);
            return reader.read(null, decoder);
        } catch (Exception ex) {
            LOGGER.error("De-Serialization exception while converting to Avro schema object", ex,
                    DataMapHolder.getLogMap());
            throw new NonRetryableErrorException(ex);
        }
    }

}