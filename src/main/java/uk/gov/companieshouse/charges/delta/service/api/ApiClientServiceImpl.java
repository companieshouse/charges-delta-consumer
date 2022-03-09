package uk.gov.companieshouse.charges.delta.service.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;


/**
 * Service that sends REST requests via private SDK.
 */
@Primary
@Service
public class ApiClientServiceImpl extends BaseApiClientServiceImpl implements ApiClientService {

    @Value("${api.internal-api-key}")
    private String chsApiKey;

    @Value("${api.api-url}")
    private String apiUrl;

    @Value("${api.internal-api-url}")
    private String internalApiUrl;

    /**
     * Construct an {@link ApiClientServiceImpl}.
     *
     * @param logger the CH logger
     */
    @Autowired
    public ApiClientServiceImpl(final Logger logger) {
        super(logger);
    }

    @Override
    public InternalApiClient getApiClient(String contextId) {
        InternalApiClient internalApiClient = new InternalApiClient(getHttpClient(contextId));
        internalApiClient.setBasePath(apiUrl);
        internalApiClient.setInternalBasePath(internalApiUrl);

        return internalApiClient;
    }

    private HttpClient getHttpClient(String contextId) {
        ApiKeyHttpClient httpClient = new ApiKeyHttpClient(chsApiKey);
        httpClient.setRequestId(contextId);
        return httpClient;
    }

    @Override
    public ApiResponse<Void> putCharge(final String log, String companyNumber,
                                       final String chargeId,
                                       InternalChargeApi internalChargeApi) {
        final String uri = String.format("/company/%s/charge/%s/internal", companyNumber, chargeId);

        Map<String, Object> logMap = createLogMap(companyNumber, "PUT", uri, chargeId);
        logger.infoContext(log, String.format("PUT %s", uri), logMap);

        return executeOp(log, "putCharge", uri,
                getApiClient(log).privateDeltaChargeResourceHandler()
                        .putCharge()
                        .upsert(uri, internalChargeApi));
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
