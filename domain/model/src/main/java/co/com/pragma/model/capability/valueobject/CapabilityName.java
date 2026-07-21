package co.com.pragma.model.capability.valueobject;

import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

public record CapabilityName(String value) {

    public static final int MAX_LENGTH = 50;

    public CapabilityName {
        Map<String, String> errors = new LinkedHashMap<>();
        validate(value, errors);
        if (!errors.isEmpty())
            throw new FieldsValidationException(errors);
    }

    public static void validate(String value, Map<String, String> errors) {
        FieldValidator.validateNotBlank(value, FieldConstants.NAME,
                ValidationMessageConstants.MSG_NAME_REQUIRED, errors);
        if (!errors.containsKey(FieldConstants.NAME))
            FieldValidator.validateMaxLength(value, MAX_LENGTH, FieldConstants.NAME,
                    String.format(ValidationMessageConstants.MSG_NAME_MAX_LENGTH, MAX_LENGTH), errors);
    }
}
