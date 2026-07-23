package co.com.pragma.model.capabilitybootcamp.valueobject;

import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BootcampCapabilityIds(List<Long> value) {

    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 4;

    public BootcampCapabilityIds {
        Map<String, String> errors = new LinkedHashMap<>();
        validate(value, errors);
        if (!errors.isEmpty())
            throw new FieldsValidationException(errors);
    }

    public static void validate(List<Long> value, Map<String, String> errors) {
        String sizeRangeMessage = String.format(ValidationMessageConstants.MSG_CAPABILITY_IDS_SIZE_RANGE, MIN_SIZE, MAX_SIZE);

        FieldValidator.validateMinSize(value, MIN_SIZE, FieldConstants.CAPABILITY_IDS, sizeRangeMessage, errors);

        if (!errors.containsKey(FieldConstants.CAPABILITY_IDS))
            FieldValidator.validateMaxSize(value, MAX_SIZE, FieldConstants.CAPABILITY_IDS, sizeRangeMessage, errors);

        if (!errors.containsKey(FieldConstants.CAPABILITY_IDS))
            FieldValidator.validateNoDuplicates(value, FieldConstants.CAPABILITY_IDS,
                    ValidationMessageConstants.MSG_CAPABILITY_IDS_DUPLICATED, errors);
    }
}
