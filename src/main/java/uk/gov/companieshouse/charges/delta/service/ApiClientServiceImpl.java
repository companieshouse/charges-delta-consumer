package uk.gov.companieshouse.charges.delta.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.Executor;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesDelete;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsert;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import java.util.Collections;
import java.util.function.Supplier;


/**
 * Service that sends REST requests via private SDK.
 */
@Primary
@Service
public class ApiClientServiceImpl implements ApiClientService {

    private static final String PRIVATE_API_GENERIC_EXCEPTION = "Private API Generic exception";
    private static final String ERROR_RESPONSE_EXCEPTION = "Private API Error Response exception";
    private static final String SDK_EXCEPTION = "SDK exception";

    private final Supplier<InternalApiClient> internalApiClientSupplier;
    private final Logger logger;

    /**
     * Construct an {@link ApiClientServiceImpl}.
     *
     * @param logger the CH logger
     */
    @Autowired
    public ApiClientServiceImpl(final Logger logger,
                                Supplier<InternalApiClient> internalApiClientSupplier) {
        this.logger = logger;
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
    public ApiResponse<Void> deleteCharge(String companyNumber, String chargeId) {
        final String formattedUri = String.format(DELETE_CHARGE_URI, companyNumber, chargeId);
        DataMapHolder.get().uri(formattedUri);

        InternalApiClient internalApiClient = internalApiClientSupplier.get();
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());

        PrivateChargesDelete executor = internalApiClient
                .privateDeltaChargeResourceHandler()
                .deleteCharge(formattedUri);

        return execute(executor);
    }

    private ApiResponse<Void> execute(Executor<ApiResponse<Void>> executor) {
        try {
            return executor.execute();
        } catch (URIValidationException ex) {
            logger.error(SDK_EXCEPTION, ex, DataMapHolder.getLogMap());
            throw new RetryableErrorException(SDK_EXCEPTION, ex);
        } catch (ApiErrorResponseException ex) {
            DataMapHolder.get().status(String.valueOf(ex.getStatusCode()));
            logger.error(ERROR_RESPONSE_EXCEPTION, ex, DataMapHolder.getLogMap());
            if (ex.getStatusCode() != 0) {
                return new ApiResponse<>(ex.getStatusCode(), Collections.emptyMap());
            }
            throw new RetryableErrorException(ERROR_RESPONSE_EXCEPTION, ex);
        } catch (Exception ex) {
            logger.error(PRIVATE_API_GENERIC_EXCEPTION, ex, DataMapHolder.getLogMap());
            throw new RetryableErrorException(PRIVATE_API_GENERIC_EXCEPTION, ex);
        }
    }
}
