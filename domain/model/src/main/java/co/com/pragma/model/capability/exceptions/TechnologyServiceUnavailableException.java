package co.com.pragma.model.capability.exceptions;

import co.com.pragma.model.exceptions.TechnicalException;

public class TechnologyServiceUnavailableException extends TechnicalException {
    public TechnologyServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
