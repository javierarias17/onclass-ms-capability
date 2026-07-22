package co.com.pragma.model.capability.query;

import co.com.pragma.model.capability.Capability;

import java.util.List;

public record CapabilityListItem(Capability capability, List<TechnologySummary> technologies) {
}
