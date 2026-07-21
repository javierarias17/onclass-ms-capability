package co.com.pragma.model.capability.exceptions;

import co.com.pragma.model.exceptions.FunctionalException;

import java.util.Map;

public class TechnologiesNotFoundException extends FunctionalException {
    public TechnologiesNotFoundException(String message, Map<String, String> errors) {
        super(message, errors);
    }
}
