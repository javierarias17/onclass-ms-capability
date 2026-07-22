package co.com.pragma.api.dto;

import java.util.List;

public record CapabilityPageOutDto(List<CapabilityListItemOutDto> content, int page, int size, long totalElements,
        int totalPages) {
}
