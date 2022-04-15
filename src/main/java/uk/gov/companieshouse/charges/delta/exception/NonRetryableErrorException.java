package uk.gov.companieshouse.charges.delta.exception;

public class NonRetryableErrorException extends RuntimeException {

    public NonRetryableErrorException(Exception ex) {
        super(ex);
    }

    public NonRetryableErrorException(String message) {
        super(message);
    }

    public NonRetryableErrorException(String message, Exception exception) {
        super(message, exception);
    }
}

