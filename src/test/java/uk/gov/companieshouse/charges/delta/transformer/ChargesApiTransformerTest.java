package uk.gov.companieshouse.charges.delta.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import scala.Char;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.charges.delta.config.TestConfig;
import uk.gov.companieshouse.charges.delta.mapper.ChargeApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.ClassificationApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.InsolvencyCasesApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.PersonsEntitledApiMapper;
import uk.gov.companieshouse.charges.delta.mapper.TransactionsApiMapper;
import uk.gov.companieshouse.charges.delta.model.TestData;
import uk.gov.companieshouse.charges.delta.processor.Encoder;

import java.io.IOException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
public class ChargesApiTransformerTest {

    @Autowired
    private ChargeApiMapper chargeApiMapper;
    @Autowired
    private Encoder encoder;

    private ChargesApiTransformer transformer;
    private TestData testData;
    ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        transformer = new ChargesApiTransformer(chargeApiMapper, encoder);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyyMMdd"));
        testData = new TestData();
    }

        /* @Test
         public void transformSuccessfully() {
             //TODO Transform ChargesDelta to InternalChargeApi model
             final Charge input = new Charge();
             assertThat(transformer.transform(input)).isEqualTo(new InternalChargeApi());
         }*/

    @Test
    @DisplayName("ChargesApiTransformer to transform Charge to InternalChargeApi mapping")
    void When_ValidChargesMessage_Expect_ValidTransformedInternal() throws IOException, JSONException {

        ChargesDelta expectedChargesDelta = testData.createChargesDelta("charges-delta-example-2.json");

        Charge charge = expectedChargesDelta.getCharges().get(0);

        String chargeJson = objectMapper.writeValueAsString(charge);

        InternalChargeApi internalChargeApi = transformer.transform(charge);
        String chargeApiJson = objectMapper.writeValueAsString(internalChargeApi.getExternalData());
        String expectedChargesApiJson = testData.loadTestdataFile("charges-api-example-2.json");
        JSONAssert.assertEquals(expectedChargesApiJson, chargeApiJson,
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("etag", (o1, o2) -> true)));


    }


}