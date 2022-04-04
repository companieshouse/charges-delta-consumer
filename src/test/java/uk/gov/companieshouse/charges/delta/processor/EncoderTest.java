package uk.gov.companieshouse.charges.delta.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(
        properties = {
                "api.charge-id-salt=jkdhjdio",
                "api.trans-id-salt=abcdefgh"
        }
)
@ExtendWith(SpringExtension.class)
class EncoderTest {

    @Value("${api.charge-id-salt}")
    private String chargeIdSalt;

    @Value("${api.trans-id-salt}")
    private String transIdSalt;

    private Encoder encoder;

    @BeforeEach
    void setup() {
        encoder = new Encoder(chargeIdSalt, transIdSalt);
    }

    @Test
    void encode() {
        String expectedValue = "CQ79y791IeRwkR3CNJMSOTNcWIA="
                .replace("+", "-")
                .replace("/", "_");
        String encodeValue = encoder.encodeWithSha1("3000117455");
        assertThat(encodeValue).isEqualTo(expectedValue);
    }

    @Test
    void encode_ChargedIdThatGeneratesAValueWithSlash() {
        String expectedValue = "0Vx0iTn_oJBRsITU1jbtTaEZElk="
                .replace("+", "-")
                .replace("/", "_");
        String encodeValue = encoder.encodeWithSha1("3001283055");
        assertThat(encodeValue).isEqualTo(expectedValue);
    }
}