package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.CapabilityInDto;
import co.com.pragma.api.dto.CapabilityListItemOutDto;
import co.com.pragma.api.dto.CapabilityOutDto;
import co.com.pragma.api.dto.CapabilityPageOutDto;
import co.com.pragma.api.dto.TechnologySummaryOutDto;
import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.command.CapabilityCreateCommand;
import co.com.pragma.model.capability.query.CapabilityListItem;
import co.com.pragma.model.capability.query.CapabilityPage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CapabilityDtoMapper {

    CapabilityCreateCommand toCapabilityCreateCommand(CapabilityInDto capabilityInDto);

    @Mapping(source = "capability.id", target = "id")
    @Mapping(source = "capability.name.value", target = "name")
    @Mapping(source = "capability.description.value", target = "description")
    @Mapping(source = "technologyIds", target = "technologyIds")
    CapabilityOutDto toCapabilityOutDto(Capability capability, List<Long> technologyIds);

    default CapabilityPageOutDto toCapabilityPageOutDto(CapabilityPage page) {
        List<CapabilityListItemOutDto> content = page.content().stream()
                .map(this::toCapabilityListItemOutDto)
                .toList();
        return new CapabilityPageOutDto(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    default CapabilityListItemOutDto toCapabilityListItemOutDto(CapabilityListItem item) {
        List<TechnologySummaryOutDto> technologies = item.technologies().stream()
                .map(technology -> new TechnologySummaryOutDto(technology.id(), technology.name()))
                .toList();
        return new CapabilityListItemOutDto(item.capability().getId(), item.capability().getName().value(),
                item.capability().getDescription().value(), technologies);
    }
}
