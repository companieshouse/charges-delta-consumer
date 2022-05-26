package uk.gov.companieshouse.charges.delta.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.gov.companieshouse.charges.delta.model.TestData;
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
    private TestData testData;
    ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        transformer = new ChargesApiTransformer(chargeApiMapper, encoderUtil, logger);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyyMMdd"));
        testData = new TestData();
    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidChargesMessage_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-2.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-2.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));
    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidChargesMessage_Expect_ValidTransformedInternal_minimum() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-3.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-3.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));
    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_Unmatched_NoticeType_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-4.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-4.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_Several_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-5.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);

        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-5.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_Different_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-6.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-6.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_MR10_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-7.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-7.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_LLRM01_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-8.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-8.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_419bScot_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-9.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-9.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_395_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-10.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-10.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_RM01_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-11.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-11.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_LLMG01s_NoticeTypes_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-12.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-12.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidMessage_With_LLMG01s_NoticeTypes_with_space_at_start_Expect_ValidTransformedInternal() throws Exception {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-source-12_notice_type_with_space_at_start.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        InternalChargeApi internalChargeApi = transformer.transform(charge, testData.createKafkaHeaders());

        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi);
        String expectedChargesApiJson = testData.loadTestdataFile("internal-charges-api-expected-12.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("external_data.etag", (o1, o2) -> true)));

    }

    @Test
    @DisplayName("Throws a Retryable error when there's an issue with the transformation")
    void When_ErrorDuringTransformation_ThenThrowRetryableErrorException() {
        Charge charge = new Charge();

        assertThrows(RetryableErrorException.class, () -> transformer.transform(charge,testData.createKafkaHeaders()));
    }
}