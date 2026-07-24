package co.com.pragma.model.capabilitybootcamp.query;

import co.com.pragma.model.capability.query.TechnologySummary;

import java.util.List;

public record CapabilitySummary(Long id, String name, List<TechnologySummary> technologies) {
}
