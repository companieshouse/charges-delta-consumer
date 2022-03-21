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
                "api.salt=jkdhjdio"
        }
)
@ExtendWith(SpringExtension.class)
class EncoderTest {

    @Value("${api.salt}")
    private String salt;
    private Encoder encoder;

    @BeforeEach
    void setup() {
        encoder = new Encoder(salt);
    }

    @Test
    void encode() {
        String expectedValue = "MDkwZWZkY2JiZjc1MjFlNDcwOTExZGMyMzQ5MzEyMzkzMzVjNTg4MA=="
                .replace("+", "-")
                .replace("/", "_");
        String encodeValue = encoder.encodeWithSha1("3000117455");
        assertThat(encodeValue).isEqualTo(expectedValue);
    }
}