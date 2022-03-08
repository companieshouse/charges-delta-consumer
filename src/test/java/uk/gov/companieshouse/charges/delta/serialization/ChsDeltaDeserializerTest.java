package uk.gov.companieshouse.charges.delta.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ChsDeltaDeserializerTest {

    @Mock
    private Logger logger;
    private ChsDeltaDeserializer deserializer;

    @BeforeEach
    public void init() {
        deserializer = new ChsDeltaDeserializer(logger);
    }

    @Test
    void When_deserialize_Expect_ValidChsDeltaObject() {
        ChsDelta chsDelta = new ChsDelta("{\"key\": \"value\"}", 1, "context_id");
        byte[] data = encodedData(chsDelta);
        ChsDelta deserializedObject = deserializer.deserialize("", data);
        assertThat(deserializedObject).isEqualTo(chsDelta);
    }

    private byte[] encodedData(ChsDelta chsDelta){
        ChsDeltaSerializer serializer = new ChsDeltaSerializer();
        return serializer.serialize("", chsDelta);
    }
}
