package uk.gov.companieshouse.charges.delta.logging;

import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.charges.delta.exception.NonRetryableErrorException;
import uk.gov.companieshouse.charges.delta.exception.RetryableErrorException;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import java.util.Optional;
import java.util.UUID;

@Component
@Aspect
class LoggingKafkaListenerAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String LOG_MESSAGE_RECEIVED = "Processing delta";
    private static final String LOG_MESSAGE_DELETE_RECEIVED = "Processing DELETE delta";
    private static final String LOG_MESSAGE_PROCESSED = "Processed delta";
    private static final String LOG_MESSAGE_DELETE_PROCESSED = "Processed DELETE delta";
    private static final String EXCEPTION_MESSAGE = "%s exception thrown";

    private final int maxAttempts;

    LoggingKafkaListenerAspect(@Value("${charges.delta.retry-attempts}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object manageStructuredLogging(ProceedingJoinPoint joinPoint) throws Throwable {

        int retryCount = 0;
        try {
            Message<?> message = (Message<?>) joinPoint.getArgs()[0];
            retryCount = Optional.ofNullable((Integer) joinPoint.getArgs()[1]).orElse(1) - 1;
            ChsDelta chsDelta = extractChsDelta(message.getPayload());
            DataMapHolder.initialise(Optional.ofNullable(chsDelta.getContextId())
                    .orElse(UUID.randomUUID().toString()));

            DataMapHolder.get()
                    .retryCount(retryCount)
                    .topic((String) joinPoint.getArgs()[2])
                    .partition((Integer) joinPoint.getArgs()[3])
                    .offset((Long) joinPoint.getArgs()[4]);

            LOGGER.info(chsDelta.getIsDelete() ? LOG_MESSAGE_DELETE_RECEIVED : LOG_MESSAGE_RECEIVED,
                    DataMapHolder.getLogMap());

            Object result = joinPoint.proceed();

            LOGGER.info(chsDelta.getIsDelete() ? LOG_MESSAGE_DELETE_PROCESSED : LOG_MESSAGE_PROCESSED,
                    DataMapHolder.getLogMap());

            return result;
        } catch (RetryableErrorException ex) {
            // maxAttempts includes first attempt which is not a retry
            if (retryCount >= maxAttempts - 1) {
                LOGGER.error("Max retry attempts reached", ex, DataMapHolder.getLogMap());
            } else {
                LOGGER.info(EXCEPTION_MESSAGE.concat(ex.getClass().getSimpleName()), DataMapHolder.getLogMap());
            }
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Exception thrown", ex, DataMapHolder.getLogMap());
            throw ex;
        } finally {
            DataMapHolder.clear();
        }
    }

    private ChsDelta extractChsDelta(Object payload) {
        if (payload.getClass().equals(ChsDelta.class)) {
            return (ChsDelta) payload;
        }
        throw new NonRetryableErrorException("Invalid payload type. payload: %s".concat(payload.toString()));
    }
}