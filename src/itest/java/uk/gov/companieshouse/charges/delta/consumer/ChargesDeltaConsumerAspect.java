package uk.gov.companieshouse.charges.delta.consumer;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.charges.delta.config.DelegatingLatch;

@Aspect
public class ChargesDeltaConsumerAspect {

    @Autowired
    private DelegatingLatch delegatingLatch;


    @After("execution(* uk.gov.companieshouse.charges.delta.consumer.ChargesDeltaConsumer.receiveMainMessages(..))")
    void receiveMainMessages() throws Throwable {
        delegatingLatch.getLatch().countDown();
    }

    @AfterThrowing("execution(* org.apache.kafka.common.serialization.Deserializer.deserialize(..))")
    public void deserialize() {
        delegatingLatch.getLatch().countDown();
    }
}
