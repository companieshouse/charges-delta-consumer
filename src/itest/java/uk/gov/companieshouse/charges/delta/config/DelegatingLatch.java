package uk.gov.companieshouse.charges.delta.config;

import java.util.concurrent.CountDownLatch;

public class DelegatingLatch {
    private CountDownLatch latch;

    public DelegatingLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }
}
