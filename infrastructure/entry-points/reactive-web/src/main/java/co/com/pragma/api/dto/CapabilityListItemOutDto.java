package co.com.pragma.api.dto;

import java.util.List;

public record CapabilityListItemOutDto(Long id, String name, String description, List<TechnologySummaryOutDto> technologies) {
}
