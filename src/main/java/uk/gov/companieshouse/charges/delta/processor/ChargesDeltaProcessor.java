package uk.gov.companieshouse.charges.delta.processor;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
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
    private EncoderUtil encoderUtil;

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

        invokeChargesDataApi(logContext, charge, internalChargeApi, logMap);
    }

    private <T> T mapToChargesDelta(ChsDelta payload, Class<T> deltaclass)
            throws NonRetryableErrorException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            T chargesDelta = mapper.readValue(payload.getData(), deltaclass);
            return chargesDelta;
        } catch (Exception exception) {
            throw new NonRetryableErrorException("Error when extracting charges delta", exception);
        }
    }

    /**
     * Invoke Charges Data API.
     */
    private void invokeChargesDataApi(final String logContext, Charge charge,
                                      InternalChargeApi internalChargeApi,
                                      final Map<String, Object> logMap) {
        final String companyNumber = charge.getCompanyNumber();

        //pass in the chargeId and encode it with base64 after doing a SHA1 hash
        final String chargeId = encoderUtil.encodeWithSha1(charge.getId());
        logger.infoContext(
                logContext,
                format("Process charge for company number [%s] and charge id [%s]",
                        companyNumber, chargeId),
                null);
        final ApiResponse<Void> response =
                apiClientService.putCharge(logContext,
                        companyNumber,
                        chargeId,
                        internalChargeApi);
        handleResponse(HttpStatus.valueOf(response.getStatusCode()), logContext, logMap);
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
        } else if (!httpStatus.is2xxSuccessful()) {
            // any other client or server status is retryable
            String message = "Non-Successful 200 response received from charges-data-api";
            logger.errorContext(logContext, message, null, logMap);
            throw new RetryableErrorException(message);
        } else {
            logger.trace("Got success response from PUT endpoint of charges-data-api");
        }
    }

    /**
     * Process Charges Delta Delete message.
     */
    public String processDelete(Message<ChsDelta> chsDelta) {
        final MessageHeaders headers = chsDelta.getHeaders();
        final ChsDelta payload = chsDelta.getPayload();
        final String logContext = payload.getContextId();
        final Map<String, Object> logMap = new HashMap<>();
        final ChargesDeleteDelta chargesDeleteDelta =
                mapToChargesDelta(payload, ChargesDeleteDelta.class);

        logger.trace(String.format("ChargesDeleteDelta extracted from Kafka message: %s",
                chargesDeleteDelta));

        String chargeId = chargesDeleteDelta.getChargesId();
        logMap.put("chargeId", chargeId);

        logger.infoContext(
                logContext,
                String.format(
                        "Process DELETE charge for charge id %s", chargeId),
                logMap);

        return chargeId;
    }

}
