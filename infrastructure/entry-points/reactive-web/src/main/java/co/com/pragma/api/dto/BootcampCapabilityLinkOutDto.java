package co.com.pragma.api.dto;

import java.util.List;

public record BootcampCapabilityLinkOutDto(Long bootcampId, List<Long> capabilityIds) {
}
