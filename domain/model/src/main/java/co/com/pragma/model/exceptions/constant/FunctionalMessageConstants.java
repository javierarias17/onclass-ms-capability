package co.com.pragma.model.exceptions.constant;

public final class FunctionalMessageConstants {

    private FunctionalMessageConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String BUSINESS_VALIDATION_FAILED = "Business validation failed";
    public static final String CAPABILITY_ALREADY_EXISTS = "Capability name already exists";
    public static final String TECHNOLOGIES_NOT_FOUND = "The following technology ids do not exist: %s";
}
