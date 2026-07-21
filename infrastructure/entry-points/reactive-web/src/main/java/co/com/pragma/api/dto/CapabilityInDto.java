package co.com.pragma.api.dto;

import java.util.List;

public record CapabilityInDto(String name, String description, List<Long> technologyIds) {
}
