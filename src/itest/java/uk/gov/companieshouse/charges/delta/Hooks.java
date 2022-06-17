package uk.gov.companieshouse.charges.delta;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.charges.delta.config.DelegatingLatch;

import java.util.concurrent.CountDownLatch;

public class Hooks {

    @Autowired
    private DelegatingLatch latch;

    @Before
    public void before() {
        latch.setLatch(new CountDownLatch(1));
    }
}
