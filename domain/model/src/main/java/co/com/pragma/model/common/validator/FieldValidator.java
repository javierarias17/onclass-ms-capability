package co.com.pragma.model.common.validator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FieldValidator {

    private FieldValidator() {
        throw new IllegalStateException("Utility class");
    }

    public static void validateNotBlank(String value, String field, String message,
                                        Map<String, String> errors) {
        if (value == null || value.isBlank())
            errors.put(field, message);
    }

    public static void validateNotNull(Object value, String field, String message,
                                       Map<String, String> errors) {
        if (value == null)
            errors.put(field, message);
    }

    public static void validateMaxLength(String value, int maxLength, String field,
                                         String message, Map<String, String> errors) {
        if (value != null && value.length() > maxLength)
            errors.put(field, message);
    }

    public static void validateNotEmpty(Collection<?> value, String field, String message,
                                        Map<String, String> errors) {
        if (value == null || value.isEmpty())
            errors.put(field, message);
    }

    public static void validateMinSize(Collection<?> value, int minSize, String field,
                                       String message, Map<String, String> errors) {
        if (value == null || value.size() < minSize)
            errors.put(field, message);
    }

    public static void validateMaxSize(Collection<?> value, int maxSize, String field,
                                       String message, Map<String, String> errors) {
        if (value != null && value.size() > maxSize)
            errors.put(field, message);
    }

    public static void validateNoDuplicates(Collection<?> value, String field, String message,
                                            Map<String, String> errors) {
        if (value == null)
            return;
        Set<Object> unique = new HashSet<>(value);
        if (unique.size() != value.size())
            errors.put(field, message);
    }
}
