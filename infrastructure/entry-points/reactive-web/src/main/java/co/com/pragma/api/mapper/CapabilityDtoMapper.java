package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.CapabilityInDto;
import co.com.pragma.api.dto.CapabilityOutDto;
import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.CapabilityCreateCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CapabilityDtoMapper {

    CapabilityCreateCommand toCommand(CapabilityInDto capabilityInDto);

    @Mapping(source = "capability.id", target = "id")
    @Mapping(source = "capability.name.value", target = "name")
    @Mapping(source = "capability.description.value", target = "description")
    @Mapping(source = "technologyIds", target = "technologyIds")
    CapabilityOutDto toResponse(Capability capability, List<Long> technologyIds);
}
