package uk.gov.companieshouse.charges.delta.processor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.delta.Charge;
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
        try {
            MessageHeaders headers = chsDelta.getHeaders();
            final ChsDelta payload = chsDelta.getPayload();
            final String logContext = payload.getContextId();
            final Map<String, Object> logMap = new HashMap<>();

            ObjectMapper mapper = new ObjectMapper();
            ChargesDelta chargesDelta = mapper.readValue(payload.getData(), ChargesDelta.class);
            logger.trace(String.format("DSND-514: ChargesDelta extracted "
                    + "from a Kafka message: %s", chargesDelta));
            if (chargesDelta.getCharges().size() > 0) {
                // assuming we always get only one charge item inside charges delta
                Charge charge = chargesDelta.getCharges().get(0);
                InternalChargeApi internalChargeApi = transformer.transform(charge, headers);

                invokeChargesDataApi(logContext, charge, internalChargeApi, logMap);
            } else {
                throw new NonRetryableErrorException("No charge item found inside ChargesDelta");
            }
        } catch (RetryableErrorException ex) {
            logger.error(String.format("DSND-684: exception "
                    + "while processing: %s", ex.getMessage()), ex);
            retryDeltaMessage(chsDelta);
        } catch (Exception ex) {
            logger.error(String.format("DSND-684: unexpected error "
                    + "while processing message: %s", ex.getMessage()), ex);
            handleErrorMessage(chsDelta);
            // send to error topic
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
                String.format("Process charge for company number [%s] and charge id [%s]",
                        companyNumber, chargeId),
                null);
        final ApiResponse<Void> response =
                apiClientService.putCharge(logContext,
                        companyNumber,
                        chargeId,
                        internalChargeApi);
        handleResponse(null, HttpStatus.valueOf(response.getStatusCode()), logContext,
                "Response received from charges data api", logMap);
    }

    private void handleResponse(
            final ResponseStatusException ex,
            final HttpStatus httpStatus,
            final String logContext,
            final String msg,
            final Map<String, Object> logMap)
            throws NonRetryableErrorException, RetryableErrorException {

        logMap.put("status", httpStatus.toString());
        if (HttpStatus.BAD_REQUEST == httpStatus) {
            // 400 BAD REQUEST status cannot be retried
            logger.errorContext(logContext, msg, null, logMap);
            throw new NonRetryableErrorException(msg);
        } else if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError()) {
            // any other client or server status can be retried
            logger.errorContext(logContext, msg + ", retry", null, logMap);
            throw new RetryableErrorException(msg);
        } else {
            logger.debugContext(logContext, msg, logMap);
        }
    }

    public void retryDeltaMessage(Message<ChsDelta> chsDelta) {

    }

    private void handleErrorMessage(Message<ChsDelta> chsDelta) {

    }

}
