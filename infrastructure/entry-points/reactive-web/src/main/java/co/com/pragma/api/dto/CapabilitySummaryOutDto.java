package co.com.pragma.api.dto;

import java.util.List;

public record CapabilitySummaryOutDto(Long id, String name, List<TechnologySummaryOutDto> technologies) {
}
