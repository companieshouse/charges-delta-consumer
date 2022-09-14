package uk.gov.companieshouse.charges.delta.processor;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDeleteDelta;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.service.ApiClientService;
import uk.gov.companieshouse.charges.delta.transformer.ChargesApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;


@Component
public class ChargesDeltaProcessor {

    private final ChargesApiTransformer transformer;
    private final Logger logger;
    private final ApiClientService apiClientService;
    private final EncoderUtil encoderUtil;

    /**
     * The constructor.
     */
    @Autowired
    public ChargesDeltaProcessor(ChargesApiTransformer transformer,
                                 Logger logger,
                                 ApiClientService apiClientService,
                                 EncoderUtil encoderUtil) {
        this.transformer = transformer;
        this.logger = logger;
        this.apiClientService = apiClientService;
        this.encoderUtil = encoderUtil;
    }

    /**
     * Process CHS Delta message.
     */
    public void processDelta(Message<ChsDelta> chsDelta) {
        final MessageHeaders headers = chsDelta.getHeaders();
        final ChsDelta payload = chsDelta.getPayload();
        final String logContext = payload.getContextId();
        final Map<String, Object> logMap = new HashMap<>();
        final ChargesDelta chargesDelta = mapToChargesDelta(payload, ChargesDelta.class);
        logger.trace(format("DSND-514: ChargesDelta extracted "
                + "from a Kafka message: %s", chargesDelta));
        if (chargesDelta.getCharges().isEmpty()) {
            throw new NonRetryableErrorException("No charge items found inside ChargesDelta");
        }

        // Assuming we always get only one charge item inside charges delta
        Charge charge = chargesDelta.getCharges().get(0);
        InternalChargeApi internalChargeApi = transformer.transform(charge, headers);

        removeBrokenFilingLinks(internalChargeApi, charge.getCompanyNumber());
        logger.info(String.format("Charge message with contextId: %s "
                + "transformed to InternalChargeApi "
                + ": %s", logContext, internalChargeApi));

        ApiResponse<Void> apiResponse = updateChargesData(logContext, charge,
                internalChargeApi, logMap);

        handleResponse(HttpStatus.valueOf(apiResponse.getStatusCode()), logContext, logMap);
    }

    /**
     * Process Charges Delta Delete message.
     */
    public String processDelete(Message<ChsDelta> chsDelta) {
        final ChsDelta payload = chsDelta.getPayload();
        final String logContext = payload.getContextId();
        final Map<String, Object> logMap = new HashMap<>();
        final ChargesDeleteDelta chargesDeleteDelta =
                mapToChargesDelta(payload, ChargesDeleteDelta.class);

        logger.trace(String.format("ChargesDeleteDelta extracted from Kafka message: %s",
                chargesDeleteDelta));

        Optional<String> chargeIdOptional = Optional.ofNullable(chargesDeleteDelta.getChargesId())
                .filter(Predicate.not(String::isEmpty));

        //pass in the chargeId and encode it with base64 after doing a SHA1 hash
        final String chargeId = encoderUtil.encodeWithSha1(chargeIdOptional.orElseThrow(
                () -> new NonRetryableErrorException("Charge Id is empty!")));

        logMap.put("company_number", 0);
        logMap.put("chargeId", chargeId);

        final ApiResponse<Void> apiResponse = deleteCharge(logContext, chargeId);

        handleDeleteResponse(HttpStatus.valueOf(apiResponse.getStatusCode()), logContext, logMap);

        return chargeId;
    }

    private <T> T mapToChargesDelta(ChsDelta payload, Class<T> deltaclass)
            throws NonRetryableErrorException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(payload.getData(), deltaclass);
        } catch (Exception exception) {
            throw new NonRetryableErrorException("Error when extracting charges delta", exception);
        }
    }

    private void removeBrokenFilingLinks(InternalChargeApi internalChargeApi,
                                         String companyNumber) {
        List<TransactionsApi> transactions = internalChargeApi.getExternalData().getTransactions();
        for (TransactionsApi transaction : transactions) {
            if (transaction.getLinks() != null) {
                if (transaction.getLinks().getFiling() != null & transaction.getLinks().getFiling()
                        .equals(String.format("/company/%s/filing-history/", companyNumber))) {
                    transaction.getLinks().setFiling(null);
                }
            }
        }
    }

    /**
     * Invoke Charges Data API to update charges database.
     */
    private ApiResponse<Void> updateChargesData(final String logContext, Charge charge,
                                   InternalChargeApi internalChargeApi,
                                   final Map<String, Object> logMap) {
        final String companyNumber = charge.getCompanyNumber();
        Optional<String> chargeIdOptional = Optional.ofNullable(charge.getId())
                .filter(Predicate.not(String::isEmpty));

        //pass in the chargeId and encode it with base64 after doing a SHA1 hash
        final String chargeId = encoderUtil.encodeWithSha1(
                chargeIdOptional.orElseThrow(
                        () -> new NonRetryableErrorException("Charge Id is empty!")));
        logMap.put("company_number", companyNumber);
        logMap.put("chargeId", chargeId);

        logger.trace(String.format("Performing a PUT with "
                + "company number %s for contextId %s", companyNumber, logContext));
        return apiClientService.putCharge(logContext,
                        companyNumber,
                        chargeId,
                        internalChargeApi);
    }

    private void handleResponse(
            final HttpStatus httpStatus,
            final String logContext,
            final Map<String, Object> logMap)
            throws NonRetryableErrorException, RetryableErrorException {

        logMap.put("status", httpStatus.toString());

        if (HttpStatus.BAD_REQUEST == httpStatus) {
            // 400 BAD REQUEST status is not retryable
            String message = "400 BAD_REQUEST response received from charges-data-api";
            logger.errorContext(logContext, message, null, logMap);
            throw new NonRetryableErrorException(message);
        }  else if (HttpStatus.NOT_FOUND == httpStatus) {
            // 404 GONE status is not retryable
            String message = "404 NOT FOUND response received from charges-data-api";
            logger.errorContext(logContext, message, null, logMap);
            throw new NonRetryableErrorException(message);
        } else if (!httpStatus.is2xxSuccessful()) {
            // any other client or server status is retryable
            String message = "Non-Successful 200 response received from charges-data-api";
            logger.errorContext(logContext, message, null, logMap);
            throw new RetryableErrorException(message);
        } else {
            logger.info(String.format("Successfully invoked charges-data-api "
                            + "PUT endpoint for message with contextId: %s",
                    logContext));
        }
    }

    /**
     * Invoke Charges Data API to update charges database.
     */
    private ApiResponse<Void> deleteCharge(final String logContext, String chargeId) {
        logger.trace(String.format("Performing DELETE with "
                + "chargeId: %s for contextId: %s", chargeId, logContext));
        return apiClientService.deleteCharge(logContext, "0", chargeId);
    }

    private void handleDeleteResponse(
            final HttpStatus httpStatus,
            final String logContext,
            final Map<String, Object> logMap)
            throws NonRetryableErrorException, RetryableErrorException {
        logMap.put("status", httpStatus.toString());
        String msg = "Response from DELETE charge request";
        Set<HttpStatus> nonRetryableStatuses =
                Collections.unmodifiableSet(EnumSet.of(
                    HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND));

        if (nonRetryableStatuses.contains(httpStatus)) {
            throw new NonRetryableErrorException(
                    String.format("Bad request DELETE Api Response %s", msg));
        } else if (!httpStatus.is2xxSuccessful()) {
            // any other client or server status is retryable
            logger.errorContext(logContext, msg + ", retry", null, logMap);
            throw new RetryableErrorException(
                    String.format("Unsuccessful DELETE API response, %s", msg));
        } else {
            logger.info(String.format("Successfully invoked charges-data-api "
                            + "DELETE endpoint for message with contextId: %s",
                    logContext));
        }
    }

}
