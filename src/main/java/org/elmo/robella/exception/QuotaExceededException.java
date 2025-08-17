package org.elmo.robella.exception;

public class QuotaExceededException extends ProviderException{
    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
