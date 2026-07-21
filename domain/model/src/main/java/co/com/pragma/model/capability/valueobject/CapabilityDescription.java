package co.com.pragma.model.capability.valueobject;

import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

public record CapabilityDescription(String value) {

    public static final int MAX_LENGTH = 90;

    public CapabilityDescription {
        Map<String, String> errors = new LinkedHashMap<>();
        validate(value, errors);
        if (!errors.isEmpty())
            throw new FieldsValidationException(errors);
    }

    public static void validate(String value, Map<String, String> errors) {
        FieldValidator.validateNotBlank(value, FieldConstants.DESCRIPTION,
                ValidationMessageConstants.MSG_DESCRIPTION_REQUIRED, errors);
        if (!errors.containsKey(FieldConstants.DESCRIPTION))
            FieldValidator.validateMaxLength(value, MAX_LENGTH, FieldConstants.DESCRIPTION,
                    String.format(ValidationMessageConstants.MSG_DESCRIPTION_MAX_LENGTH, MAX_LENGTH), errors);
    }
}
