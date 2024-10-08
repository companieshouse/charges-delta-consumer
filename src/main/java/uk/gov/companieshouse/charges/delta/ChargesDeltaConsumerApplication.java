package uk.gov.companieshouse.charges.delta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChargesDeltaConsumerApplication {

    public static final String NAMESPACE = "charges-delta-consumer";
    public static void main(String[] args) {
        SpringApplication.run(ChargesDeltaConsumerApplication.class, args);
    }
}
