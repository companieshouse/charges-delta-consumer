package uk.gov.companieshouse.charges.delta.mapper;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;

@Aspect
@Component
public class DescriptiveChargeApiMapperAspect {

    @Autowired
    private Logger logger;

    @AfterReturning(value = "execution(* uk.gov.companieshouse.charges.delta.mapper"
            + ".TextFormatter.*(..))", returning = "mappedValue")
    void logMapping(JoinPoint joinPoint, Object mappedValue) {
        logger.trace(String.format("Mapped [%s] to [%s]", joinPoint.getArgs()[0], mappedValue));
    }
}
