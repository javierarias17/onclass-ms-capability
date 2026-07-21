package co.com.pragma.api.dto;

import java.util.List;

public record CapabilityOutDto(Long id, String name, String description, List<Long> technologyIds) {
}
