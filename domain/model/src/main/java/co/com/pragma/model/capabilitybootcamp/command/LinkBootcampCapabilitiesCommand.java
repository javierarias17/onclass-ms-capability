package co.com.pragma.model.capabilitybootcamp.command;

import java.util.List;

public record LinkBootcampCapabilitiesCommand(Long bootcampId, List<Long> capabilityIds) {
}
