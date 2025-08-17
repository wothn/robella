package org.elmo.robella.exception;

public class RateLimitException extends ProviderException {
    public RateLimitException(String message, Throwable cause) {
        super(message,  cause);
    }
}
