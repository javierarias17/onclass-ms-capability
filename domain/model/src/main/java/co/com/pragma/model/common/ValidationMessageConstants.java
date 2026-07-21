package co.com.pragma.model.common;

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
}
