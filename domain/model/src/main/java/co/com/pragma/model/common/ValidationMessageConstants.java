package co.com.pragma.model.common;

import co.com.pragma.model.capability.query.CapabilitySortFieldEnum;
import co.com.pragma.model.capability.query.SortDirectionEnum;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class ValidationMessageConstants {

    private ValidationMessageConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String MSG_NAME_REQUIRED = "Capability name is required";
    public static final String MSG_NAME_MAX_LENGTH = "Capability name must not exceed %d characters";
    public static final String MSG_DESCRIPTION_REQUIRED = "Capability description is required";
    public static final String MSG_DESCRIPTION_MAX_LENGTH = "Capability description must not exceed %d characters";
    public static final String MSG_TECHNOLOGY_IDS_MIN_SIZE = "Capability must have at least %d technologies";
    public static final String MSG_TECHNOLOGY_IDS_MAX_SIZE = "Capability must have at most %d technologies";
    public static final String MSG_TECHNOLOGY_IDS_DUPLICATED = "Capability technology ids must not contain duplicates";
    public static final String MSG_PAGE_MUST_BE_NUMERIC = "Page must be numeric";
    public static final String MSG_PAGE_OUT_OF_RANGE = "Page must be zero or greater";
    public static final String MSG_SIZE_MUST_BE_NUMERIC = "Size must be numeric";
    public static final String MSG_SIZE_OUT_OF_RANGE = "Size must be between %d and %d";
    public static final String MSG_SORT_BY_INVALID = "Sort field must be one of: " + allowedNames(CapabilitySortFieldEnum.values());
    public static final String MSG_SORT_DIRECTION_INVALID = "Sort direction must be one of: " + allowedNames(SortDirectionEnum.values());

    private static String allowedNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
    }
}
