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
                "api.charge-id-salt=sometest",
                "api.trans-id-salt=sometest2"
        }
)
@ExtendWith(SpringExtension.class)
class EncoderUtilTest {

    @Value("${api.charge-id-salt}")
    private String chargeIdSalt;

    @Value("${api.trans-id-salt}")
    private String transIdSalt;

    private EncoderUtil encoderUtil;

    @BeforeEach
    void setup() {
        encoderUtil = new EncoderUtil(chargeIdSalt, transIdSalt);
    }

    @Test
    void encode() {
        String expectedValue = "2e6X44GGkwsvYqCd5HF6a42mWZs="
                .replace("+", "-")
                .replace("/", "_");
        String encodeValue = encoderUtil.encodeWithSha1("3000117455");
        assertThat(encodeValue).isEqualTo(expectedValue);
    }

    @Test
    void encodeWithoutSha1() {
        String expectedValue = "MzAwMDExNzQ1NXNvbWV0ZXN0Mg==";
        String encodeValue = encoderUtil.encodeWithoutSha1("3000117455");
        assertThat(encodeValue).isEqualTo(expectedValue);
    }

    @Test
    void encode_charged_Id_which_generates_value_with_slash() {
        String expectedValue = "613cXqSXAA1Ce_fpkFQ9ZP_L5ZQ=";
        String encodeValue = encoderUtil.encodeWithSha1("3101283055");
        assertThat(encodeValue).isEqualTo(expectedValue);


    }

}