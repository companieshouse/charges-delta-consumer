package uk.gov.companieshouse.charges.delta.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.charges.delta.config.TestConfig;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.util.TestSupport;
import uk.gov.companieshouse.charges.delta.processor.EncoderUtil;
import uk.gov.companieshouse.logging.Logger;

import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
public class ChargesApiTransformerTest {

    @Autowired
    private ChargeApiMapper chargeApiMapper;
    @Autowired
    private EncoderUtil encoderUtil;

    @Mock
    private Logger logger;

    private ChargesApiTransformer transformer;
    private TestSupport testSupport;
    ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        transformer = new ChargesApiTransformer(chargeApiMapper, encoderUtil, logger);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyyMMdd"));
        testSupport = new TestSupport();
    }


    private static Stream<Arguments> jsonFileSourceNames() {
        return Stream.of(
            Arguments.of("charges-delta-source-2.json", "internal-charges-api-expected-2.json"),
            Arguments.of("charges-delta-source-3.json", "internal-charges-api-expected-3.json"),
            Arguments.of("charges-delta-source-4.json", "internal-charges-api-expected-4.json"),
            Arguments.of("charges-delta-source-5.json", "internal-charges-api-expected-5.json"),
            Arguments.of("charges-delta-source-6.json", "internal-charges-api-expected-6.json"),
            Arguments.of("charges-delta-source-7.json", "internal-charges-api-expected-7.json"),
            Arguments.of("charges-delta-source-8.json", "internal-charges-api-expected-8.json"),
            Arguments.of("charges-delta-source-9.json", "internal-charges-api-expected-9.json"),
            Arguments.of("charges-delta-source-10.json", "internal-charges-api-expected-10.json"),
            Arguments.of("charges-delta-source-11.json", "internal-charges-api-expected-11.json"),
            Arguments.of("charges-delta-source-12.json", "internal-charges-api-expected-12.json"),
            Arguments.of("charges-delta-source-13_empty_transactionId.json",
                    "internal-charges-api-expected-13_empty_transactionId.json"),
            Arguments.of("charges-delta-source-14_empty_case_transactionId.json",
                    "internal-charges-api-expected-14_empty_case_transactionId.json"),
            Arguments.of("charges-delta-source-15_case_transactionId.json",
                        "internal-charges-api-expected-15_case_transactionId.json"),
            Arguments.of("charges-delta-source-16_case_transactionId.json",
                        "internal-charges-api-expected-16_case_transactionId.json"),
            Arguments.of("charges-delta-source-empty-dates.json", "internal-charges-api-expected-empty-dates.json"),
            Arguments.of("charges-delta-source-17.json", "internal-charges-api-expected-17.json"),
                Arguments.of("charges-delta-source-18.json", "internal-charges-api-expected-18.json")
        );
    }

    @MethodSource("jsonFileSourceNames")
    @ParameterizedTest
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidChargesMessage_Expect_ValidTransformedInternal(String source, String expected) throws Exception {

        ChargesDelta expectedChargesDelta = testSupport.createChargesDelta(source);

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testSupport.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        System.out.println("chargeApiJson = " + chargeApiJson);
        String expectedChargesApiJson = testSupport.loadTestdataFile(expected);
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
            new CustomComparator(JSONCompareMode.STRICT_ORDER,
                new Customization("external_data.etag", (o1, o2) -> true)));
    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_LLMG01s_NoticeTypes_with_space_at_start_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testSupport.createChargesDelta("charges-delta-source-12_notice_type_with_space_at_start.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testSupport.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testSupport.loadTestdataFile("internal-charges-api-expected-12.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.STRICT_ORDER,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("Throws a Retryable error when there's an issue with the transformation")
    void When_ErrorDuringTransformation_ThenThrowRetryableErrorException() {
        Charge charge = new Charge();

        assertThrows(RetryableErrorException.class, () -> transformer.transform(charge, testSupport.createKafkaHeaders()));
    }
}