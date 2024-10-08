
package uk.gov.companieshouse.charges.delta.service;

import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.model.ApiResponse;

/**
 * The {@code ApiClientService} interface provides an abstraction that can be
 * used when testing {@code ApiClientManager} static methods, without imposing
 * the use of a test framework that supports mocking of static methods.
 */
public interface ApiClientService {

    String PUT_CHARGE_URI = "/company/%s/charge/%s/internal";
    String DELETE_CHARGE_URI = "/company/%s/charges/%s";

    /**
     * Submit charge.
     */
    ApiResponse<Void> putCharge(
            final String companyNumber,
            final String chargeId,
            final InternalChargeApi internalChargeApi);

    /**
     * Delete charge.
     */
    ApiResponse<Void> deleteCharge(
            final String companyNumber,
            final String chargeId);
}
