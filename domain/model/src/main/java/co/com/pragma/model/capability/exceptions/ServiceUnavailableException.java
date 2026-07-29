package co.com.pragma.model.capability.exceptions;

import co.com.pragma.model.exceptions.TechnicalException;

public class ServiceUnavailableException extends TechnicalException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
