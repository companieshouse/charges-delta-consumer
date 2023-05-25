package uk.gov.companieshouse.charges.delta.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.Executor;
import uk.gov.companieshouse.api.handler.delta.PrivateDeltaResourceHandler;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesDelete;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsert;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesUpsertResourceHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;


/**
 * Service that sends REST requests via private SDK.
 */
@Primary
@Service
public class ApiClientServiceImpl implements ApiClientService {

    //private Supplier<InternalApiClient> internalApiClientSupplier;
    private Logger logger;

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
    public ApiResponse<Void> putCharge(final String log, String companyNumber,
                                       final String chargeId,
                                       InternalChargeApi internalChargeApi) {
        final String uri = String.format(PUT_CHARGE_URI, companyNumber, chargeId);

        Map<String, Object> logMap = createLogMap(companyNumber, "PUT", uri, chargeId);
        logger.infoContext(log, String.format("PUT %s", uri), logMap);
        InternalApiClient internalApiClient = internalApiClientSupplier.get();
        internalApiClient.getHttpClient().setRequestId(log);

        PrivateDeltaResourceHandler privateDeltaResourceHandler =
                internalApiClient.privateDeltaChargeResourceHandler();
        PrivateChargesUpsertResourceHandler privateChargesUpsertResourceHandler
                = privateDeltaResourceHandler.putCharge();
        PrivateChargesUpsert privateChargesUpsert =
                privateChargesUpsertResourceHandler.upsert(uri, internalChargeApi);
        return execute(log, logMap, privateChargesUpsert);
    }

    @Override
    public ApiResponse<Void> deleteCharge(String log, String companyNumber,
                                          String chargeId) {
        final String uri =
                String.format(DELETE_CHARGE_URI, companyNumber, chargeId);

        Map<String,Object> logMap = createLogMap(companyNumber,"DELETE", uri, chargeId);
        logger.infoContext(log, String.format("DELETE %s", uri), logMap);

        InternalApiClient internalApiClient = internalApiClientSupplier.get();
        internalApiClient.getHttpClient().setRequestId(log);

        PrivateDeltaResourceHandler privateDeltaResourceHandler =
                internalApiClient.privateDeltaChargeResourceHandler();
        PrivateChargesDelete executor = privateDeltaResourceHandler.deleteCharge(uri);

        return execute(log, logMap, executor);

    }

    private ApiResponse<Void> execute(String log, Map<String, Object> logMap,
                                      Executor<ApiResponse<Void>> executor) {
        try {
            return executor.execute();
        } catch (URIValidationException ex) {
            logger.errorContext(log, "SDK exception", ex, logMap);
            throw new RetryableErrorException("SDK Exception", ex);
        } catch (ApiErrorResponseException ex) {
            String message = "Private API Error Response exception";
            logMap.put("status", ex.getStatusCode());
            logger.errorContext(log, message, ex, logMap);
            if (ex.getStatusCode() != 0) {
                return new ApiResponse<>(ex.getStatusCode(), Collections.emptyMap());
            }
            throw new RetryableErrorException(message, ex);
        } catch (Exception ex) {
            String message = "Private API Generic exception";
            logger.errorContext(log, message, ex, logMap);
            throw new RetryableErrorException(message, ex);
        }
    }

    private Map<String, Object> createLogMap(String companyNumber, String method, String path,
                                             String chargeId) {
        final Map<String, Object> logMap = new HashMap<>();
        logMap.put("company_number", companyNumber);
        logMap.put("charge_id", chargeId);
        logMap.put("method", method);
        logMap.put("path", path);
        return logMap;
    }



}
