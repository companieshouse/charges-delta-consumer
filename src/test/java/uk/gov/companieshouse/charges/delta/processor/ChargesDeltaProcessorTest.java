package uk.gov.companieshouse.charges.delta.processor;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.model.TestData;
import uk.gov.companieshouse.charges.delta.producer.ChargesDeltaProducer;
import uk.gov.companieshouse.charges.delta.service.api.ApiClientService;
import uk.gov.companieshouse.charges.delta.transformer.ChargesApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChargesDeltaProcessorTest {

    private ChargesDeltaProcessor deltaProcessor;

    @Mock
    private ChargesDeltaProducer chargesDeltaProducer;

    @Mock
    private ChargesApiTransformer transformer;

    @Mock
    private Logger logger;

    @Mock
    private ApiClientService apiClientService;

    private Encoder encoder;

    private TestData testData;

    @BeforeEach
    void setUp() {
        encoder = new Encoder("some_salt");
        deltaProcessor = new ChargesDeltaProcessor(chargesDeltaProducer, transformer, logger, apiClientService, encoder);
        testData = new TestData();
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta")
    void When_ValidChsDeltaMessage_Expect_ValidChargesDeltaMapping() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = testData.createChsDeltaMessage("charges-delta-example.json");
        ChargesDelta expectedChargesDelta = testData.createChargesDelta();
        Charge charge = expectedChargesDelta.getCharges().get(0);
        when(transformer.transform(charge)).thenCallRealMethod();
        deltaProcessor.processDelta(mockChsDeltaMessage);
        verify(transformer).transform(charge);
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta with no items")
    void When_InValidChsDeltaMessage_Expect_Exception() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = testData.createChsDeltaMessage("charges-delta-example-no-charge.json");
        deltaProcessor.processDelta(mockChsDeltaMessage);
        verifyNoInteractions(transformer);
    }

    @Test
    @DisplayName("Transforms ChsDelta payload into an ChargesDelta and then calls Charges Data Api")
    void When_ValidChsDeltaMessage_Invoke_Data_Api_And_Get_Response() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = testData.createChsDeltaMessage("charges-delta-example.json");
        ChargesDelta expectedChargesDelta = testData.createChargesDelta();
        Charge charge = expectedChargesDelta.getCharges().get(0);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        when(transformer.transform(charge)).thenCallRealMethod();
        when(apiClientService.putCharge(eq("context_id"), eq("01099198"),
                eq("ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA=="), eq(testData.mockInternalChargeApi())))
                .thenReturn(response);
        deltaProcessor.processDelta(mockChsDeltaMessage);
        verify(transformer).transform(charge);
        verify(apiClientService).putCharge("context_id", "01099198",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA==", testData.mockInternalChargeApi());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }


}
