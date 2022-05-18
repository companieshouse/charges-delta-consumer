package uk.gov.companieshouse.charges.delta.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesDelete;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsert;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class ApiClientServiceImplTest {

    @Mock
    Logger logger;

    private ApiClientServiceImpl apiClientService;

    @BeforeEach
    void setup() {
        apiClientService = new ApiClientServiceImpl(logger);
        ReflectionTestUtils.setField(apiClientService, "chsApiKey", "apiKey");
        ReflectionTestUtils.setField(apiClientService, "apiUrl", "https://api.companieshouse.gov.uk");
    }

    @Test
    void putCharge() throws ApiErrorResponseException, URIValidationException {
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        ApiClientServiceImpl apiClientService2 = Mockito.spy(apiClientService);
        doReturn(response).when(apiClientService2).executeOp(anyString(), anyString(),
                anyString(),
                any(PrivateChargesUpsert.class));

        ApiResponse<Void> response2 = apiClientService2.putCharge("context_id", "12345678",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                new InternalChargeApi());
        verify(apiClientService2).executeOp(anyString(), eq("putCharge"),
                eq("/company/12345678/charge/" +
                        "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA/internal"),
                any(PrivateChargesUpsert.class));

        assertThat(response2).isEqualTo(response);

    }

    @Test
    void putCharge_receives_error() {
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        ApiClientServiceImpl apiClientService2 = Mockito.spy(apiClientService);
        doThrow(RetryableErrorException.class).when(apiClientService2).executeOp(anyString(), anyString(),
                anyString(),
                any(PrivateChargesUpsert.class));

        assertThrows(RetryableErrorException.class,
                () -> apiClientService2.putCharge("context_id", "12345678",
                        "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA",
                        new InternalChargeApi()));

    }

    @Test
    void deleteCharge() throws ApiErrorResponseException, URIValidationException {
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        ApiClientServiceImpl apiClientService2 = Mockito.spy(apiClientService);
        doReturn(response).when(apiClientService2).executeOp(anyString(), anyString(),
                anyString(),
                any(PrivateChargesDelete.class));

        ApiResponse<Void> response2 = apiClientService2.deleteCharge("context_id", "0",
                "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA");
        verify(apiClientService2).executeOp(anyString(), eq("deleteCharge"),
                eq("/company/0/charges/" +
                        "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA"),
                any(PrivateChargesDelete.class));

        assertThat(response2).isEqualTo(response);

    }

    @Test
    void deleteCharge_receives_error() throws ApiErrorResponseException, URIValidationException {
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        ApiClientServiceImpl apiClientService2 = Mockito.spy(apiClientService);
        doThrow(RetryableErrorException.class).when(apiClientService2).executeOp(anyString(), anyString(),
                anyString(),
                any(PrivateChargesDelete.class));

        assertThrows(RetryableErrorException.class,
                () -> apiClientService2.deleteCharge("context_id", "0",
                        "ZTgzYWQwODAzMGY1ZDNkNGZiOTAxOWQ1YzJkYzc5MWViMTE3ZjQxZA"));

    }

}