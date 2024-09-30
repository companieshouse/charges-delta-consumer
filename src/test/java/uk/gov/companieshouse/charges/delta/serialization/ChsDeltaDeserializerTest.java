package uk.gov.companieshouse.charges.delta.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.delta.ChsDelta;

@ExtendWith(MockitoExtension.class)
class ChsDeltaDeserializerTest {

    private ChsDeltaDeserializer deserializer;

    @BeforeEach
    public void init() {
        deserializer = new ChsDeltaDeserializer();
    }

    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void When_deserialize_Expect_ValidChsDeltaObject(boolean isDelete) {
        ChsDelta chsDelta = new ChsDelta("{\"key\": \"value\"}", 1, "context_id", isDelete);
        byte[] data = encodedData(chsDelta);

        ChsDelta deserializedObject = deserializer.deserialize("", data);

        assertThat(deserializedObject).isEqualTo(chsDelta);
    }

    @Test
    void When_deserializeFails_throwsNonRetryableError() {
        byte[] data = "Invalid message".getBytes();
        assertThrows(NonRetryableErrorException.class, () -> deserializer.deserialize("", data));
    }

    private byte[] encodedData(ChsDelta chsDelta) {
        ChsDeltaSerializer serializer = new ChsDeltaSerializer();
        return serializer.serialize("", chsDelta);
    }
}
