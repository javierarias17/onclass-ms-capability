package co.com.pragma.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CapabilityInDto(String name, String description, List<Long> technologyIds) {
}
