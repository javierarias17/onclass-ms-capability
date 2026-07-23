package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.BootcampCapabilityLinkInDto;
import co.com.pragma.api.dto.BootcampCapabilityLinkOutDto;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.command.LinkBootcampCapabilitiesCommand;
import org.mapstruct.Mapper;

import java.util.List;

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
}
