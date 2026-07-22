package co.com.pragma.model.capability.command;

import java.util.List;

public record CapabilityCreateCommand(String name, String description, List<Long> technologyIds) {
}
