package org.elmo.robella.exception;

public class AuthenticationException extends ProviderException {
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
