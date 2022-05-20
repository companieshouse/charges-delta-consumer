package uk.gov.companieshouse.charges.delta.processor;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.util.TestSupport;
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

    private TestSupport testSupport;

    @BeforeEach
    void setUp() {
        encoderUtil = new EncoderUtil("some_salt", "transId_salt");
        deltaProcessor = new ChargesDeltaProcessor(transformer, logger, apiClientService, encoderUtil);
        testSupport = new TestSupport();
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta")
    void When_ValidChsDeltaMessage_Expect_ValidChargesDelta() throws IOException {
        Message<ChsDelta> testChsDeltaMessage = testSupport.createChsDeltaMessage("charges-delta-source-1.json", false);
        ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        when(transformer.transform(any(Charge.class), any(MessageHeaders.class))).thenReturn(testSupport.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        deltaProcessor.processDelta(testChsDeltaMessage);
        verify(transformer).transform(any(Charge.class), any(MessageHeaders.class));
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDelta with no items")
    void When_InValidChsDeltaMessage_Expect_Exception() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testSupport.createChsDeltaMessage("charges-delta-source-no-charge.json", false);
        assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verifyNoInteractions(transformer);
    }

    @Test
    @DisplayName("Transforms ChsDelta payload into an ChargesDelta and then calls Charges Data Api")
    void When_ValidChsDeltaMessage_Invoke_Data_Api_And_Get_Response() throws IOException {
        Message<ChsDelta> testChsDeltaMessage = testSupport.createChsDeltaMessage("charges-delta-source-1.json", false);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        when(transformer.transform(any(Charge.class), any(MessageHeaders.class))).thenReturn(testSupport.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(eq("context_id"), eq("01099198"),
                eq("6DrQgDD109T7kBnVwtx5HrEX9B0"), eq(testSupport.mockInternalChargeApi()));

        deltaProcessor.processDelta(testChsDeltaMessage);
        verify(transformer).transform(any(Charge.class), any(MessageHeaders.class));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0", testSupport.mockInternalChargeApi());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Bad request when calling put charges, throws non retryable error")
    void When_PutChargesBadRequest_NonRetryableError() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testSupport.createChsDeltaMessage("charges-delta-source-1.json", false);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), null, null);

        when(transformer.transform(any(), any())).thenReturn(testSupport.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0", testSupport.mockInternalChargeApi());
    }

    @Test
    @DisplayName("Getting another 4xx when calling put charges, throws retryable error")
    void When_PutChargesUnauthorized_RetryableError() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testSupport.createChsDeltaMessage("charges-delta-source-1.json", false);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), null, null);

        when(transformer.transform(any(), any())).thenReturn(testSupport.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        assertThrows(RetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0", testSupport.mockInternalChargeApi());
    }

    @Test
    @DisplayName("Getting internal server error when calling put charges API, throws retryable error")
    void When_PutChargesApiInternalServerError_RetryableError() throws IOException {
        Message<ChsDelta> chsDeltaMessage = testSupport.createChsDeltaMessage("charges-delta-source-1.json", false);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), null, null);

        when(transformer.transform(any(), any())).thenReturn(testSupport.mockInternalChargeApi());
        doReturn(response).when(apiClientService).putCharge(any(), any(), any(), any());

        assertThrows(RetryableErrorException.class, () -> deltaProcessor.processDelta(chsDeltaMessage));
        verify(apiClientService).putCharge("context_id", "01099198",
                "6DrQgDD109T7kBnVwtx5HrEX9B0", testSupport.mockInternalChargeApi());
    }

    @Test
    @DisplayName("When can't transform into charges delta API, throws retryable error")
    void When_CantTransformIntoChargesDeltaApi_RetryableError() throws IOException {
        Message<ChsDelta> invalidChsDeltaMessage = testSupport.createInvalidChsDeltaMessage(false);
        assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelta(invalidChsDeltaMessage));
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDeleteDelta and calling delete endpoint")
    void When_ValidChsDeltaDelete_Message_Expect_Valid_ChargesDeleteResponse() throws IOException {
        Message<ChsDelta> mockChsChargesDeleteDeltaMessage = testSupport.createChsDeltaMessage(
                "charges-delete-delta-source-1.json", true);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        doReturn(response).when(apiClientService).deleteCharge(eq("context_id"), eq("0"),
                eq("yt6cQ-A2DqNpqwAMDWxKX12Axv4"));
        String chargeId = deltaProcessor.processDelete(mockChsChargesDeleteDeltaMessage);
        Assertions.assertEquals("yt6cQ-A2DqNpqwAMDWxKX12Axv4", chargeId);

        verify(apiClientService).deleteCharge("context_id", "0",
                "yt6cQ-A2DqNpqwAMDWxKX12Axv4");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("When mapping an invalid ChsDelta message into Charges Delete Delta then throws a non-retryable exception")
    void When_Invalid_ChargesDeleteDelta_nonRetryableError() throws IOException {
        Message<ChsDelta> invalidChsChargesDeltaDeltaMessage = testSupport.createInvalidChsDeltaMessage(true);
        Assertions.assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelete(invalidChsChargesDeltaDeltaMessage));
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDeleteDelta and calling delete endpoint")
    void When_ValidChsDeltaDelete_Message_And_ChargesDelete_Endpoint_Gives_Error() throws IOException {
        Message<ChsDelta> mockChsChargesDeleteDeltaMessage = testSupport.createChsDeltaMessage(
                "charges-delete-delta-source-1.json", true);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), null, null);
        doReturn(response).when(apiClientService).deleteCharge(eq("context_id"), eq("0"),
                eq("yt6cQ-A2DqNpqwAMDWxKX12Axv4"));

        assertThrows(RetryableErrorException.class, () -> deltaProcessor.processDelete(mockChsChargesDeleteDeltaMessage));
        verify(apiClientService).deleteCharge("context_id", "0",
                "yt6cQ-A2DqNpqwAMDWxKX12Axv4");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an ChargesDeleteDelta and calling delete endpoint")
    void When_Delete_Non_Existing_Charge_ErrorResponse() throws IOException {
        Message<ChsDelta> mockChsChargesDeleteDeltaMessage = testSupport.createChsDeltaMessage(
                "charges-delete-delta-source-1.json", true);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.NOT_FOUND.value(), null, null);
        doReturn(response).when(apiClientService).deleteCharge(eq("context_id"), eq("0"),
                eq("yt6cQ-A2DqNpqwAMDWxKX12Axv4"));

        assertThrows(NonRetryableErrorException.class, () -> deltaProcessor.processDelete(mockChsChargesDeleteDeltaMessage));
        verify(apiClientService).deleteCharge("context_id", "0",
                "yt6cQ-A2DqNpqwAMDWxKX12Axv4");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    private static Stream<Arguments> provideExceptionParameters() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, NonRetryableErrorException.class),
                Arguments.of(HttpStatus.NOT_FOUND, NonRetryableErrorException.class),
                Arguments.of(HttpStatus.UNAUTHORIZED, RetryableErrorException.class),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, RetryableErrorException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    @DisplayName("When calling DELETE charge and an error occurs then throw the appropriate exception based on the error type")
    void When_Delete_Exception_Then_Throw_Appropriate_Exception(HttpStatus httpStatus,
                                                                Class<Throwable> exception)
            throws IOException {
        Message<ChsDelta> mockChsChargesDeleteDeltaMessage = testSupport.createChsDeltaMessage(
                "charges-delete-delta-source-1.json", true);
        final ApiResponse<Void> response = new ApiResponse<>(httpStatus.value(), null, null);
        doReturn(response).when(apiClientService).deleteCharge(eq("context_id"), eq("0"),
                eq("yt6cQ-A2DqNpqwAMDWxKX12Axv4"));

        assertThrows(exception, () -> deltaProcessor.processDelete(mockChsChargesDeleteDeltaMessage));
        verify(apiClientService).deleteCharge("context_id", "0",
                "yt6cQ-A2DqNpqwAMDWxKX12Axv4");
        assertThat(response.getStatusCode()).isEqualTo(httpStatus.value());
    }

}
