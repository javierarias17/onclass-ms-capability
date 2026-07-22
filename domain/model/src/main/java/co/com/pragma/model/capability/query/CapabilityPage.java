package co.com.pragma.model.capability.query;

import java.util.List;

public record CapabilityPage(List<CapabilityListItem> content, int page, int size, long totalElements,
        int totalPages) {
}
