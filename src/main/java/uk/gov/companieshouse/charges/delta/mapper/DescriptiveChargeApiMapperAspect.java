package uk.gov.companieshouse.charges.delta.mapper;

import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.charges.delta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Aspect
@Component
public class DescriptiveChargeApiMapperAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @AfterReturning(value = "execution(* uk.gov.companieshouse.charges.delta.mapper"
            + ".TextFormatter.*(..))", returning = "mappedValue")
    void logMapping(JoinPoint joinPoint, Object mappedValue) {
        LOGGER.trace(String.format("Mapped [%s] to [%s]", joinPoint.getArgs()[0], mappedValue), DataMapHolder.getLogMap());
    }
}
