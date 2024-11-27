package uk.gov.companieshouse.charges.delta.service;

import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.Executor;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesDelete;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsert;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import java.util.Arrays;
import java.util.function.Supplier;


/**
 * Service that sends REST requests via private SDK.
 */
@Primary
@Service
public class ApiClientServiceImpl implements ApiClientService {

    private static final String API_INFO_RESPONSE_MESSAGE = "Call to API failed, status code: %d. %s";
    private static final String API_ERROR_RESPONSE_MESSAGE = "Call to API failed, status code: %d";
    private static final String URI_VALIDATION_EXCEPTION_MESSAGE = "Invalid URI";

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final Supplier<InternalApiClient> internalApiClientSupplier;

    /**
     * Construct an {@link ApiClientServiceImpl}.
     */
    @Autowired
    public ApiClientServiceImpl(Supplier<InternalApiClient> internalApiClientSupplier) {
        this.internalApiClientSupplier = internalApiClientSupplier;
    }

    @Override
    public ApiResponse<Void> putCharge(String companyNumber,
                                       final String chargeId,
                                       InternalChargeApi internalChargeApi) {
        final String formattedUri = String.format(PUT_CHARGE_URI, companyNumber, chargeId);
        DataMapHolder.get().uri(formattedUri);

        InternalApiClient internalApiClient = internalApiClientSupplier.get();
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());

        PrivateChargesUpsert privateChargesUpsert =
                internalApiClient
                        .privateDeltaChargeResourceHandler()
                        .putCharge()
                        .upsert(formattedUri, internalChargeApi);

        return execute(privateChargesUpsert);
    }

    @Override
    public ApiResponse<Void> deleteCharge(String companyNumber, String chargeId, String deltaAt) {
        final String formattedUri = String.format(DELETE_CHARGE_URI, companyNumber, chargeId);
        DataMapHolder.get().uri(formattedUri);

        InternalApiClient internalApiClient = internalApiClientSupplier.get();
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());

        PrivateChargesDelete executor = internalApiClient
                .privateDeltaChargeResourceHandler()
                .deleteCharge(formattedUri, deltaAt);

        return execute(executor);
    }

    private ApiResponse<Void> execute(Executor<ApiResponse<Void>> executor) {
        try {
            return executor.execute();
        } catch (URIValidationException ex) {
            LOGGER.error(URI_VALIDATION_EXCEPTION_MESSAGE, ex, DataMapHolder.getLogMap());
            throw new RetryableErrorException(URI_VALIDATION_EXCEPTION_MESSAGE, ex);
        } catch (ApiErrorResponseException ex) {
            final int statusCode = ex.getStatusCode();
            final HttpStatus httpStatus = HttpStatus.valueOf(ex.getStatusCode());
            DataMapHolder.get().status(String.valueOf(statusCode));

            if (HttpStatus.CONFLICT.equals(httpStatus) || HttpStatus.BAD_REQUEST.equals(httpStatus)) {
                LOGGER.error(String.format(API_ERROR_RESPONSE_MESSAGE, statusCode), ex, DataMapHolder.getLogMap());
                throw new NonRetryableErrorException(String.format(API_ERROR_RESPONSE_MESSAGE, statusCode), ex);
            } else {
                LOGGER.info(String.format(API_INFO_RESPONSE_MESSAGE, statusCode, Arrays.toString(ex.getStackTrace())), DataMapHolder.getLogMap());
                throw new RetryableErrorException(API_INFO_RESPONSE_MESSAGE, ex);
            }
        }
    }
}
