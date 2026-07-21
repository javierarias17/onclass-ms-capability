package co.com.pragma.model.capability;

import java.util.List;

public record CapabilityCreateCommand(String name, String description, List<Long> technologyIds) {
}
