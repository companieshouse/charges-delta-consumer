package uk.gov.companieshouse.charges.delta.serialization;

import java.util.Arrays;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;

@Component
public class ChsDeltaDeserializer implements Deserializer<ChsDelta> {

    private final Logger logger;

    @Autowired
    public ChsDeltaDeserializer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public ChsDelta deserialize(String topic, byte[] data) {
        try {
            logger.trace(String.format("Message picked up from topic with data: %s",
                    new String(data)));
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<ChsDelta> reader = new ReflectDatumReader<>(ChsDelta.class);
            ChsDelta chsDelta = reader.read(null, decoder);
            logger.trace(String.format("Message successfully de-serialised into "
                    + "Avro ChsDelta object: %s", chsDelta));
            return chsDelta;
        } catch (Exception ex) {
            logger.error("Serialization exception while converting to Avro schema object", ex);
            throw new SerializationException(
                    "Message data [" + Arrays.toString(data) + "] from topic [" + topic + "] "
                            + "cannot be deserialized", ex);
        }
    }

}
