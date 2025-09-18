package org.elmo.robella.exception;

public class EmailAlreadyExistsException extends UserException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}