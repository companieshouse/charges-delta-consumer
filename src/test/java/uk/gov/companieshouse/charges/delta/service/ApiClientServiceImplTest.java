package uk.gov.companieshouse.charges.delta.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.delta.PrivateDeltaResourceHandler;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesDelete;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsert;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsertResourceHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestPropertySource(
        properties = {
                "api.charges-data-api-key=apiKey1",
                "api.api-url=http://localhost:7070"
        }
)
@ExtendWith(MockitoExtension.class)
class ApiClientServiceImplTest {

    @Value("${api.charges-data-api-key}")
    private String apiKey;
    @Value("${api.api-url}")
    private String apiUrl;

    @Mock
    Logger logger;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateDeltaResourceHandler privateDeltaResourceHandler;

    @Mock
    private PrivateChargesDelete privateChargesDelete;

    @Mock
    private ApiResponse<Void> response;

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private PrivateChargesUpsertResourceHandler privateChargesUpsertResourceHandler;

    @Mock
    private PrivateChargesUpsert privateChargesUpsert;
    @Mock
    HttpClient httpClient;

    @InjectMocks
    private ApiClientServiceImpl apiClientService;

    @Mock
    InternalChargeApi internalChargeApi;

    @BeforeEach
    void setup() {

    }

    @Test
    void putCharge() throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.putCharge()).thenReturn(privateChargesUpsertResourceHandler);
        when(privateChargesUpsertResourceHandler.upsert(anyString(), any(InternalChargeApi.class)))
                .thenReturn(privateChargesUpsert);
        when(privateChargesUpsert.execute()).thenReturn(response);

        ApiResponse<?> apiResponse = apiClientService.putCharge(
                "context_id", "12345678",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                internalChargeApi);

        Assertions.assertThat(apiResponse).isNotNull();

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .putCharge();
        verify(privateChargesUpsertResourceHandler, times(1))
                .upsert(anyString(), any(InternalChargeApi.class));
        verify(privateChargesUpsert, times(1))
                .execute();

    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters2")
    @DisplayName("When calling DELETE charge and an error occurs then throw the appropriate exception based on the error type")
    void When_PutCharge_Exception_Then_Throw_Appropriate_Exception(HttpStatus httpStatus,
            ApiErrorResponseException exceptionFromApi)
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.putCharge()).thenReturn(privateChargesUpsertResourceHandler);
        when(privateChargesUpsertResourceHandler.upsert(anyString(), any(InternalChargeApi.class)))
                .thenReturn(privateChargesUpsert);
        when(privateChargesUpsert.execute()).thenThrow(exceptionFromApi);

        ApiResponse<?> apiResponse = assertDoesNotThrow(() -> apiClientService.putCharge(
                "context_id", "12345678",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                internalChargeApi));

        Assertions.assertThat(apiResponse).isNotNull();
        Assertions.assertThat(apiResponse.getStatusCode()).isEqualTo(httpStatus.value());

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .putCharge();
        verify(privateChargesUpsertResourceHandler, times(1))
                .upsert(anyString(), any(InternalChargeApi.class));
        verify(privateChargesUpsert, times(1))
                .execute();

    }

    @Test
    void deleteCharge() throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.deleteCharge(Mockito.anyString())).thenReturn(privateChargesDelete);
        when(privateChargesDelete.execute()).thenReturn(response);

        ApiResponse<?> apiResponse = apiClientService.deleteCharge(
                "LOG_CONTEXT", "0111", "test");

        Assertions.assertThat(apiResponse).isNotNull();

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .deleteCharge(Mockito.anyString());
        verify(privateChargesDelete, times(1))
                .execute();

    }

    private static Stream<Arguments> provideExceptionParameters2() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, buildApiErrorResponseException(HttpStatus.BAD_REQUEST)),
                Arguments.of(HttpStatus.NOT_FOUND, buildApiErrorResponseException(HttpStatus.NOT_FOUND)),
                Arguments.of(HttpStatus.UNAUTHORIZED, buildApiErrorResponseException(HttpStatus.UNAUTHORIZED)),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, buildApiErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR)),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, buildApiErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR))
        );
    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters2")
    @DisplayName("When calling DELETE charge and an error occurs then throw the appropriate exception based on the error type")
    void When_Delete_Exception_Then_Throw_Appropriate_Exception(HttpStatus httpStatus,
            ApiErrorResponseException exceptionFromApi)
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.deleteCharge(Mockito.anyString())).thenReturn(privateChargesDelete);
        when(privateChargesDelete.execute()).thenThrow(exceptionFromApi);

        ApiResponse<?> apiResponse = assertDoesNotThrow(() -> apiClientService.deleteCharge(
                "LOG_CONTEXT", "0", "test"));

        Assertions.assertThat(apiResponse).isNotNull();
        Assertions.assertThat(apiResponse.getStatusCode()).isEqualTo(httpStatus.value());

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .deleteCharge(Mockito.anyString());
        verify(privateChargesDelete, times(1))
                .execute();

    }

    @Test
    @DisplayName("When calling DELETE charge and URLValidation exception occurs then throw the appropriate exception based on the error type")
    void When_Delete_Exception_Then_Throw_Appropriate_Exception()
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.deleteCharge(Mockito.anyString())).thenReturn(privateChargesDelete);
        when(privateChargesDelete.execute()).thenThrow(URIValidationException.class);

        assertThrows(RetryableErrorException.class, () -> apiClientService.deleteCharge(
                "LOG_CONTEXT", "0", "test"));

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .deleteCharge(Mockito.anyString());
        verify(privateChargesDelete, times(1))
                .execute();

    }

    private static ApiErrorResponseException buildApiErrorResponseException(HttpStatus httpStatus) {

        final HttpResponseException httpResponseException = new HttpResponseException.Builder(
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                new HttpHeaders()
        ).build();

        return ApiErrorResponseException.fromHttpResponseException(httpResponseException);
    }

    private static ApiErrorResponseException buildApiErrorResponseCustomException(int nonHttpStatusCode) {

        final HttpResponseException httpResponseException = new HttpResponseException.Builder(
                nonHttpStatusCode,
                "some error",
                new HttpHeaders()
        ).build();

        return ApiErrorResponseException.fromHttpResponseException(httpResponseException);
    }

    @Test
    @DisplayName("When calling PUT charge and URLValidation exception occurs then throw the appropriate exception based on the error type")
    void When_Put_Exception_Then_Throw_Appropriate_Exception()
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.putCharge()).thenReturn(privateChargesUpsertResourceHandler);
        when(privateChargesUpsertResourceHandler.upsert(anyString(), any(InternalChargeApi.class)))
                .thenReturn(privateChargesUpsert);
        when(privateChargesUpsert.execute()).thenThrow(URIValidationException.class);

        assertThrows(RetryableErrorException.class, () -> apiClientService.putCharge(
                "context_id", "12345678",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                internalChargeApi));

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .putCharge();
        verify(privateChargesUpsert, times(1))
                .execute();

    }

    @Test
    @DisplayName("When calling PUT charge and APIResponse returns 0 status code")
    void When_Put_Exception_With_Status_Code_0_Then_Throw_Appropriate_Exception()
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.putCharge()).thenReturn(privateChargesUpsertResourceHandler);
        when(privateChargesUpsertResourceHandler.upsert(anyString(), any(InternalChargeApi.class)))
                .thenReturn(privateChargesUpsert);
        when(privateChargesUpsert.execute()).thenThrow(buildApiErrorResponseCustomException(0));
        assertThrows(RetryableErrorException.class, () -> apiClientService.putCharge(
                "context_id", "12345678",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                internalChargeApi));

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .putCharge();
        verify(privateChargesUpsert, times(1))
                .execute();

    }

    @Test
    @DisplayName("When calling DELETE charge and APIResponse returns 0 status code")
    void When_Delete_Exception_With_Status_Code_0_Then_Throw_Appropriate_Exception()
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.deleteCharge(Mockito.anyString())).thenReturn(privateChargesDelete);
        when(privateChargesDelete.execute()).thenThrow(buildApiErrorResponseCustomException(0));

        assertThrows(RetryableErrorException.class, () -> apiClientService.deleteCharge(
                "LOG_CONTEXT", "0", "test"));

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .deleteCharge(Mockito.anyString());
        verify(privateChargesDelete, times(1))
                .execute();


    }
}