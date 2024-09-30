package uk.gov.companieshouse.charges.delta.service;

import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

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
import uk.gov.companieshouse.logging.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final Supplier<InternalApiClient> internalApiClientSupplier;

    /**
     * Construct an {@link ApiClientServiceImpl}.
     *
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
            LOGGER.error(SDK_EXCEPTION, ex, DataMapHolder.getLogMap());
            throw new RetryableErrorException(SDK_EXCEPTION, ex);
        } catch (ApiErrorResponseException ex) {
            DataMapHolder.get().status(String.valueOf(ex.getStatusCode()));
            LOGGER.error(ERROR_RESPONSE_EXCEPTION, ex, DataMapHolder.getLogMap());
            if (ex.getStatusCode() != 0) {
                return new ApiResponse<>(ex.getStatusCode(), Collections.emptyMap());
            }
            throw new RetryableErrorException(ERROR_RESPONSE_EXCEPTION, ex);
        } catch (Exception ex) {
            LOGGER.error(PRIVATE_API_GENERIC_EXCEPTION, ex, DataMapHolder.getLogMap());
            throw new RetryableErrorException(PRIVATE_API_GENERIC_EXCEPTION, ex);
        }
    }
}
