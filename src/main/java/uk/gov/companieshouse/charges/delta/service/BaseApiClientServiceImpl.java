package uk.gov.companieshouse.charges.delta.service;

import java.util.HashMap;
import java.util.Map;

import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.Executor;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

public abstract class BaseApiClientServiceImpl {
    protected Logger logger;

    protected BaseApiClientServiceImpl(final Logger logger) {
        this.logger = logger;
    }

    /**
     * General execution of an SDK endpoint.
     *
     * @param <T>           type of api response
     * @param logContext    context ID for logging
     * @param operationName name of operation
     * @param uri           uri of sdk being called
     * @param executor      executor to use
     * @return the response object
     */
    public <T> ApiResponse<T> executeOp(final String logContext,
                                        final String operationName,
                                        final String uri,
                                        final Executor<ApiResponse<T>> executor) {

        final Map<String, Object> logMap = new HashMap<>();
        logMap.put("operation_name", operationName);
        logMap.put("path", uri);

        try {
            return executor.execute();
        } catch (ApiErrorResponseException ex) {
            String message = "Private API Error Response exception";
            logMap.put("status", ex.getStatusCode());
            logger.errorContext(logContext, message, ex, logMap);
            throw new RetryableErrorException(message, ex);
        } catch (Exception ex) {
            String message = "Private API Generic exception";
            logger.errorContext(logContext, message, ex, logMap);
            throw new RetryableErrorException(message, ex);
        }
    }
}
