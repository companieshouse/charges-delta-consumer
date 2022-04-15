
package uk.gov.companieshouse.charges.delta.service;

import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.model.ApiResponse;

/**
 * The {@code ApiClientService} interface provides an abstraction that can be
 * used when testing {@code ApiClientManager} static methods, without imposing
 * the use of a test framework that supports mocking of static methods.
 */
public interface ApiClientService {

    InternalApiClient getApiClient(String contextId);

    /**
     * Submit charge.
     */
    ApiResponse<Void> putCharge(
            final String log,
            final String companyNumber,
            final String chargeId,
            final InternalChargeApi internalChargeApi);
}
