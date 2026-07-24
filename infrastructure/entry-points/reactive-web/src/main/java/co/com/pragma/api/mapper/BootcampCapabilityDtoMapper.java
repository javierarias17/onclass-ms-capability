package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.BootcampCapabilityLinkInDto;
import co.com.pragma.api.dto.BootcampCapabilityLinkOutDto;
import co.com.pragma.api.dto.CapabilitiesByBootcampEntryOutDto;
import co.com.pragma.api.dto.CapabilitiesByBootcampOutDto;
import co.com.pragma.api.dto.CapabilitySummaryOutDto;
import co.com.pragma.api.dto.TechnologySummaryOutDto;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.command.LinkBootcampCapabilitiesCommand;
import co.com.pragma.model.capabilitybootcamp.query.CapabilitySummary;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface BootcampCapabilityDtoMapper {

    LinkBootcampCapabilitiesCommand toLinkBootcampCapabilitiesCommand(BootcampCapabilityLinkInDto bootcampCapabilityLinkInDto);

    default BootcampCapabilityLinkOutDto toBootcampCapabilityLinkOutDto(List<CapabilityBootcamp> capabilityBootcamps) {
        if (capabilityBootcamps == null || capabilityBootcamps.isEmpty())
            return new BootcampCapabilityLinkOutDto(null, List.of());

        return new BootcampCapabilityLinkOutDto(capabilityBootcamps.get(0).getBootcampId(),
                capabilityBootcamps.stream()
                        .map(CapabilityBootcamp::getCapabilityId)
                        .toList());
    }

    default CapabilitiesByBootcampOutDto toCapabilitiesByBootcampOutDto(Map<Long, List<CapabilitySummary>> capabilitiesByBootcamp) {
        List<CapabilitiesByBootcampEntryOutDto> entries = capabilitiesByBootcamp.entrySet().stream()
                .map(entry -> new CapabilitiesByBootcampEntryOutDto(entry.getKey(),
                        entry.getValue().stream()
                                .map(this::toCapabilitySummaryOutDto)
                                .toList()))
                .toList();
        return new CapabilitiesByBootcampOutDto(entries);
    }

    default CapabilitySummaryOutDto toCapabilitySummaryOutDto(CapabilitySummary capabilitySummary) {
        List<TechnologySummaryOutDto> technologies = capabilitySummary.technologies().stream()
                .map(technology -> new TechnologySummaryOutDto(technology.id(), technology.name()))
                .toList();
        return new CapabilitySummaryOutDto(capabilitySummary.id(), capabilitySummary.name(), technologies);
    }
}
