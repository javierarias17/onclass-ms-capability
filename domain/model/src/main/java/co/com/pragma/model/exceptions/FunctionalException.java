package co.com.pragma.model.exceptions;

import lombok.Getter;

import java.util.Map;

@Getter
public abstract class FunctionalException extends RuntimeException {

    private final String message;
    private final Map<String, String> errors;

    protected FunctionalException(String message, Map<String, String> errors) {
        super();
        this.message = message;
        this.errors = errors;
    }
}
