package co.com.pragma.r2dbc.mapper;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.CapabilityStatusEnum;
import co.com.pragma.r2dbc.entity.CapabilityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CapabilityEntityMapper {

    @Mapping(source = "name.value", target = "name")
    @Mapping(source = "description.value", target = "description")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "technologyCount", target = "technologyCount")
    CapabilityEntity toEntity(Capability capability);

    default Capability toDomain(CapabilityEntity entity) {
        return entity == null ? null
                : Capability.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .description(entity.getDescription())
                        .status(CapabilityStatusEnum.valueOf(entity.getStatus()))
                        .technologyCount(entity.getTechnologyCount())
                        .version(entity.getVersion())
                        .build();
    }
}
