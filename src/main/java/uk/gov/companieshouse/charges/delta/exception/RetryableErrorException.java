package uk.gov.companieshouse.charges.delta.exception;

public class RetryableErrorException extends RuntimeException {
    public RetryableErrorException(String message) {
        super(message);
    }

    public RetryableErrorException(String message, Exception exception) {
        super(message, exception);
    }

    public RetryableErrorException(Exception exception) {
        super(exception);
    }
}

