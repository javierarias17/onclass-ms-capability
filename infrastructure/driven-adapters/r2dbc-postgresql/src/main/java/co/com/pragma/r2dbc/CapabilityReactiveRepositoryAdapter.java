package co.com.pragma.r2dbc;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.exceptions.CapabilityAlreadyExistsException;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.exceptions.constant.FunctionalMessageConstants;
import co.com.pragma.r2dbc.entity.CapabilityEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.mapper.CapabilityEntityMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;

@Repository
public class CapabilityReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Capability,
        CapabilityEntity,
        Long,
        CapabilityReactiveRepository
        > implements CapabilityRepository {

    private final CapabilityEntityMapper capabilityEntityMapper;

    public CapabilityReactiveRepositoryAdapter(CapabilityReactiveRepository repository, ObjectMapper mapper,
                                               CapabilityEntityMapper capabilityEntityMapper) {
        super(repository, mapper, capabilityEntityMapper::toDomain);
        this.capabilityEntityMapper = capabilityEntityMapper;
    }

    @Override
    protected CapabilityEntity toData(Capability capability) {
        return capabilityEntityMapper.toEntity(capability);
    }

    @Override
    public Mono<Capability> save(Capability capability) {
        return super.save(capability)
                .onErrorMap(DuplicateKeyException.class, ex -> new CapabilityAlreadyExistsException(
                        FunctionalMessageConstants.BUSINESS_VALIDATION_FAILED,
                        Map.of(FieldConstants.NAME, FunctionalMessageConstants.CAPABILITY_ALREADY_EXISTS)));
    }

    @Override
    public Mono<Capability> findByName(String name) {
        return repository.findByNameIgnoreCase(name).map(this::toEntity);
    }
}
