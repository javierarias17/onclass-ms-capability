package co.com.pragma.api.dto;

import java.util.List;

public record CapabilityExistenceOutDto(List<Long> missingIds) {
}
