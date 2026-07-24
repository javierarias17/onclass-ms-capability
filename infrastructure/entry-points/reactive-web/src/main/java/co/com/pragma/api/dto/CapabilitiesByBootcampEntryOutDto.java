package co.com.pragma.api.dto;

import java.util.List;

public record CapabilitiesByBootcampEntryOutDto(Long bootcampId, List<CapabilitySummaryOutDto> capabilities) {
}
