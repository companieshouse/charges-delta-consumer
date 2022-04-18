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
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.model.TestData;
import uk.gov.companieshouse.charges.delta.service.ApiClientService;
import uk.gov.companieshouse.charges.delta.transformer.ChargesApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class ChargesDeltaProcessorTest {

    private ChargesDeltaProcessor deltaProcessor;

    @Mock
    private ChargesApiTransformer transformer;

    @Mock
    private Logger logger;

    @Mock
    private ApiClientService apiClientService;

    private EncoderUtil encoderUtil;

    private TestData testData;

    @BeforeEach
    void setUp() {
        encoderUtil = new EncoderUtil("some_salt", "transId_salt");
        deltaProcessor = new ChargesDeltaProcessor(transformer, logger, apiClientService, encoderUtil);
        testData = new TestData();
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta")
    void When_ValidChsDeltaMessage_Expect_ValidChargesDelta() throws IOException {
        Message<ChsDelta> testChsDeltaMessage = testData.createChsDeltaMessage("charges-delta-source-1.json");
        ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        when(transformer.transform(any(Charge.class), any(MessageHeaders.class))).thenReturn(testData.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        deltaProcessor.processDelta(testChsDeltaMessage);
        verify(transformer).transform(any(Charge.class), any(MessageHeaders.class));
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta with no items")
    void When_InValidChsDeltaMessage_Expect_Exception() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testData.createChsDeltaMessage("charges-delta-source-no-charge.json");
        assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verifyNoInteractions(transformer);
    }

    @Test
    @DisplayName("Transforms ChsDelta payload into an ChargesDelta and then calls Charges Data Api")
    void When_ValidChsDeltaMessage_Invoke_Data_Api_And_Get_Response() throws IOException {
        Message<ChsDelta> testChsDeltaMessage = testData.createChsDeltaMessage("charges-delta-source-1.json");
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        when(transformer.transform(any(Charge.class), any(MessageHeaders.class))).thenReturn(testData.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(eq("context_id"), eq("01099198"),
                eq("6DrQgDD109T7kBnVwtx5HrEX9B0="), eq(testData.mockInternalChargeApi()));

        deltaProcessor.processDelta(testChsDeltaMessage);
        verify(transformer).transform(any(Charge.class), any(MessageHeaders.class));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0=", testData.mockInternalChargeApi());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Bad request when calling put charges, throws non retryable error")
    void When_PutChargesBadRequest_NonRetryableError() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testData.createChsDeltaMessage("charges-delta-source-1.json");
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), null, null);

        when(transformer.transform(any(), any())).thenReturn(testData.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0=", testData.mockInternalChargeApi());
    }

    @Test
    @DisplayName("Getting another 4xx when calling put charges, throws retryable error")
    void When_PutChargesUnauthorized_RetryableError() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testData.createChsDeltaMessage("charges-delta-source-1.json");
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), null, null);

        when(transformer.transform(any(), any())).thenReturn(testData.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        assertThrows(RetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0=", testData.mockInternalChargeApi());
    }

    @Test
    @DisplayName("Getting internal server error when calling put charges API, throws retryable error")
    void When_PutChargesApiInternalServerError_RetryableError() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testData.createChsDeltaMessage("charges-delta-source-1.json");
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), null, null);

        when(transformer.transform(any(), any())).thenReturn(testData.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        assertThrows(RetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0=", testData.mockInternalChargeApi());
    }

    @Test
    @DisplayName("When can't transform into charges delta API, throws retryable error")
    void When_CantTransformIntoChargesDeltaApi_RetryableError() throws IOException {
        Message<ChsDelta> invalidChsDeltaMessage = testData.createInvalidChsDeltaMessage();
        assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelta(invalidChsDeltaMessage));
    }
}
