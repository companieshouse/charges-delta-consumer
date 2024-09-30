package uk.gov.companieshouse.charges.delta.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.delta.ChsDelta;

@ExtendWith(MockitoExtension.class)
class ChsDeltaSerializerTest {

    private ChsDeltaSerializer serializer;

    @BeforeEach
    public void init() {
        serializer = new ChsDeltaSerializer();
    }

    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void When_serialize_Expect_chsDeltaBytes(boolean isDelete) {
        ChsDelta chsDelta = new ChsDelta("{\"key\": \"value\"}", 1, "context_id", isDelete);

        byte[] result = serializer.serialize("", chsDelta);

        assertThat(decodedData(result)).isEqualTo(chsDelta);
    }

    @Test
    void When_serialize_null_returns_null() {
        byte[] serialize = serializer.serialize("", null);
        assertThat(serialize).isNull();
    }

    @Test
    void When_serialize_receivesBytes_returnsBytes() {
        byte[] byteExample = "Example bytes".getBytes();
        byte[] serialize = serializer.serialize("", byteExample);
        assertThat(serialize).isEqualTo(byteExample);
    }

    private ChsDelta decodedData(byte[] chsDelta) {
        ChsDeltaDeserializer serializer = new ChsDeltaDeserializer();
        return serializer.deserialize("", chsDelta);
    }
}
