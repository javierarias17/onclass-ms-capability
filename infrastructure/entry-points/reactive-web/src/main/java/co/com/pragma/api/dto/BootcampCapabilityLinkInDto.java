package co.com.pragma.api.dto;

import java.util.List;

public record BootcampCapabilityLinkInDto(Long bootcampId, List<Long> capabilityIds) {
}
