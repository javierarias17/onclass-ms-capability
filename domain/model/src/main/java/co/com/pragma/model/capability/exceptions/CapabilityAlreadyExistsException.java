package co.com.pragma.model.capability.exceptions;

import co.com.pragma.model.exceptions.FunctionalException;

import java.util.Map;

public class CapabilityAlreadyExistsException extends FunctionalException {
    public CapabilityAlreadyExistsException(String message, Map<String, String> errors) {
        super(message, errors);
    }
}
