package uk.gov.companieshouse.charges.delta.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.delta.PrivateDeltaResourceHandler;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesDelete;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsert;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsertResourceHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;

@TestPropertySource(
        properties = {
                "api.charges-data-api-key=apiKey1",
                "api.api-url=http://localhost:7070"
        }
)
@ExtendWith(MockitoExtension.class)
class ApiClientServiceImplTest {
    private static final String COMPANY_NUMBER = "12345678";
    private static final String CHARGE_ID = "ABC134DEF5678";
    private static final String DELTA_AT = "20140925171003950844";

    @Value("${api.charges-data-api-key}")
    private String apiKey;
    @Value("${api.api-url}")
    private String apiUrl;

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
                "12345678",
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
    @MethodSource("provideExceptionParameters")
    @DisplayName("When calling DELETE charge and an error occurs then throw the appropriate exception based on the error type")
    void When_PutCharge_Exception_Then_Throw_Appropriate_Exception(int statusCode)
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.putCharge()).thenReturn(privateChargesUpsertResourceHandler);
        when(privateChargesUpsertResourceHandler.upsert(anyString(), any(InternalChargeApi.class)))
                .thenReturn(privateChargesUpsert);
        when(privateChargesUpsert.execute()).thenReturn(new ApiResponse<>(statusCode, null));

        ApiResponse<?> apiResponse = assertDoesNotThrow(() -> apiClientService.putCharge(
                "12345678",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                internalChargeApi));

        Assertions.assertThat(apiResponse).isNotNull();
        Assertions.assertThat(apiResponse.getStatusCode()).isEqualTo(statusCode);

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
        when(privateDeltaResourceHandler.deleteCharge(anyString(), anyString())).thenReturn(privateChargesDelete);
        when(privateChargesDelete.execute()).thenReturn(response);

        ApiResponse<?> apiResponse = apiClientService.deleteCharge(COMPANY_NUMBER, CHARGE_ID, DELTA_AT);

        Assertions.assertThat(apiResponse).isNotNull();

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1)).deleteCharge(anyString(), anyString());
        verify(privateChargesDelete, times(1))
                .execute();

    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    @DisplayName("When calling DELETE charge and an error occurs then throw the appropriate exception based on the error type")
    void When_Delete_Exception_Then_Throw_Appropriate_Exception(int statusCode)
            throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
        when(privateDeltaResourceHandler.deleteCharge(anyString(), anyString())).thenReturn(privateChargesDelete);
        when(privateChargesDelete.execute()).thenReturn(new ApiResponse<>(statusCode, null));

        ApiResponse<?> apiResponse = assertDoesNotThrow(() ->
                apiClientService.deleteCharge(COMPANY_NUMBER, CHARGE_ID, DELTA_AT));

        Assertions.assertThat(apiResponse).isNotNull();
        Assertions.assertThat(apiResponse.getStatusCode()).isEqualTo(statusCode);

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1)).deleteCharge(anyString(), anyString());
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
        when(privateDeltaResourceHandler.deleteCharge(anyString(), anyString())).thenReturn(privateChargesDelete);
        when(privateChargesDelete.execute()).thenThrow(URIValidationException.class);

        assertThrows(RetryableErrorException.class, () ->
                apiClientService.deleteCharge(COMPANY_NUMBER, CHARGE_ID, DELTA_AT));

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1)).deleteCharge(anyString(), anyString());
        verify(privateChargesDelete, times(1))
                .execute();

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
                "12345678",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                internalChargeApi));

        verify(internalApiClient, times(1)).getHttpClient();
        verify(internalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .putCharge();
        verify(privateChargesUpsert, times(1))
                .execute();

    }

    private static Stream<Arguments> provideExceptionParameters() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST.value()),
                Arguments.of(HttpStatus.NOT_FOUND.value()),
                Arguments.of(HttpStatus.UNAUTHORIZED.value()),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR.value())
        );
    }
}