package co.com.pragma.model.exceptions;

import co.com.pragma.model.exceptions.constant.FunctionalMessageConstants;

import java.util.Map;

public class FieldsValidationException extends FunctionalException {
    public FieldsValidationException(Map<String, String> errors) {
        super(FunctionalMessageConstants.BUSINESS_VALIDATION_FAILED, errors);
    }
}
