package uk.gov.companieshouse.charges.delta.processor;

import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.delta.Charge;
import uk.gov.companieshouse.api.delta.ChargesDeleteDelta;
import uk.gov.companieshouse.api.delta.ChargesDelta;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.charges.delta.logging.DataMapHolder;
import uk.gov.companieshouse.charges.delta.service.ApiClientService;
import uk.gov.companieshouse.charges.delta.transformer.ChargesApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;


@Component
public class ChargesDeltaProcessor {

    public static final String NON_RETRYABLE_RESPONSE_ERROR_MESSAGE = "Non-retryable response %s from charges-data-api";
    public static final String RETRYABLE_RESPONSE_ERROR_MESSAGE = "Retryable response %s from charges-data-api";

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final ChargesApiTransformer transformer;
    private final ApiClientService apiClientService;
    private final EncoderUtil encoderUtil;
    private final Set<HttpStatus> nonRetryableStatuses =
            Collections.unmodifiableSet(EnumSet.of(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.CONFLICT));

    /**
     * The constructor.
     */
    public ChargesDeltaProcessor(ChargesApiTransformer transformer,
                                 ApiClientService apiClientService,
                                 EncoderUtil encoderUtil) {
        this.transformer = transformer;
        this.apiClientService = apiClientService;
        this.encoderUtil = encoderUtil;
    }

    /**
     * Process CHS Delta message.
     */
    public void processDelta(Message<ChsDelta> chsDelta) {
        final ChargesDelta chargesDelta =
                mapToChargesDelta(chsDelta.getPayload(), ChargesDelta.class);
        if (chargesDelta.getCharges().isEmpty()) {
            throw new NonRetryableErrorException("No charge items found inside ChargesDelta");
        }

        // Assuming we always get only one charge item inside charges delta
        Charge charge = chargesDelta.getCharges().get(0);
        String rawChargeId = Optional.ofNullable(charge.getId()).filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new NonRetryableErrorException("Charge Id is empty"));

        DataMapHolder.get().mortgageId(rawChargeId);
        DataMapHolder.get().companyNumber(charge.getCompanyNumber());

        InternalChargeApi internalChargeApi = transformer.transform(charge, chsDelta.getHeaders());

        removeBrokenFilingLinks(internalChargeApi, charge.getCompanyNumber());

        ApiResponse<Void> apiResponse = updateChargesData(rawChargeId, charge, internalChargeApi);

        handleResponse(HttpStatus.valueOf(apiResponse.getStatusCode()));
    }

    /**
     * Process Charges Delta Delete message.
     */
    public void processDelete(Message<ChsDelta> chsDelta) {
        final ChargesDeleteDelta chargesDeleteDelta =
                mapToChargesDelta(chsDelta.getPayload(), ChargesDeleteDelta.class);
        DataMapHolder.get().mortgageId(chargesDeleteDelta.getChargesId());

        Optional<String> chargeIdOptional = Optional.ofNullable(chargesDeleteDelta.getChargesId())
                .filter(Predicate.not(String::isEmpty));

        //pass in the chargeId and encode it with base64 after doing a SHA1 hash
        final String chargeId = encoderUtil.encodeWithSha1(chargeIdOptional.orElseThrow(
                () -> new NonRetryableErrorException("Charge Id is empty!")));

        final ApiResponse<Void> apiResponse = deleteCharge(chargeId);

        handleDeleteResponse(HttpStatus.valueOf(apiResponse.getStatusCode()));
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
            if (transaction.getLinks() != null && transaction.getLinks().getFiling() != null && transaction.getLinks().getFiling()
                        .equals(String.format("/company/%s/filing-history/", companyNumber))) {
                    transaction.getLinks().setFiling(null);
                }

        }
    }

    /**
     * Invoke Charges Data API to update charges database.
     */
    private ApiResponse<Void> updateChargesData(String rawChargeId, Charge charge, InternalChargeApi internalChargeApi) {

        //pass in the chargeId and encode it with base64 after doing a SHA1 hash
        final String chargeId = encoderUtil.encodeWithSha1(rawChargeId);

        return apiClientService.putCharge(charge.getCompanyNumber(), chargeId, internalChargeApi);
    }

    private void handleResponse(final HttpStatus httpStatus)
            throws NonRetryableErrorException, RetryableErrorException {
        DataMapHolder.get().status(httpStatus.toString());

        if (httpStatus.is2xxSuccessful()) {
            LOGGER.info("Successfully invoked charges-data-api PUT endpoint",
                    DataMapHolder.getLogMap());
        } else if (HttpStatus.CONFLICT == httpStatus || HttpStatus.BAD_REQUEST == httpStatus) {
            String message = String.format(NON_RETRYABLE_RESPONSE_ERROR_MESSAGE,
                    httpStatus);
            LOGGER.error(message, null, DataMapHolder.getLogMap());
            throw new NonRetryableErrorException(message);
        } else {
            String message = String.format(RETRYABLE_RESPONSE_ERROR_MESSAGE,
                    httpStatus);
            LOGGER.info(message, DataMapHolder.getLogMap());
            throw new RetryableErrorException(message);
        }
    }

    /**
     * Invoke Charges Data API to update charges database.
     */
    private ApiResponse<Void> deleteCharge(String chargeId) {
        return apiClientService.deleteCharge("0", chargeId);
    }

    private void handleDeleteResponse(final HttpStatus httpStatus) {

        if (nonRetryableStatuses.contains(httpStatus)) {
            LOGGER.error(String.format(NON_RETRYABLE_RESPONSE_ERROR_MESSAGE,
                    httpStatus), DataMapHolder.getLogMap());
            throw new NonRetryableErrorException(String.format(NON_RETRYABLE_RESPONSE_ERROR_MESSAGE,
                    httpStatus));
        } else if (!httpStatus.is2xxSuccessful()) {
            LOGGER.info(String.format(RETRYABLE_RESPONSE_ERROR_MESSAGE, httpStatus),
                    DataMapHolder.getLogMap());
            throw new RetryableErrorException(String.format(RETRYABLE_RESPONSE_ERROR_MESSAGE,
                    httpStatus));
        } else {
            LOGGER.info("Successfully invoked charges-data-api DELETE endpoint",
                    DataMapHolder.getLogMap());
        }
    }
}
