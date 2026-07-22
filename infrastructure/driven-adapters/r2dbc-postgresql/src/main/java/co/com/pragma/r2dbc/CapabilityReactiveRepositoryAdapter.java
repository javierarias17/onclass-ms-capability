package co.com.pragma.r2dbc;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.query.CapabilitySortFieldEnum;
import co.com.pragma.model.capability.query.SortDirectionEnum;
import co.com.pragma.model.capability.exceptions.CapabilityAlreadyExistsException;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.exceptions.constant.FunctionalMessageConstants;
import co.com.pragma.r2dbc.entity.CapabilityEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.mapper.CapabilityEntityMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Repository
public class CapabilityReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Capability,
        CapabilityEntity,
        Long,
        CapabilityReactiveRepository
        > implements CapabilityRepository {

    private static final String STATUS_COLUMN = "status";

    private final CapabilityEntityMapper capabilityEntityMapper;
    private final R2dbcEntityTemplate template;

    public CapabilityReactiveRepositoryAdapter(CapabilityReactiveRepository repository, ObjectMapper mapper,
                                               CapabilityEntityMapper capabilityEntityMapper,
                                               R2dbcEntityTemplate template) {
        super(repository, mapper, capabilityEntityMapper::toDomain);
        this.capabilityEntityMapper = capabilityEntityMapper;
        this.template = template;
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

    @Override
    public Mono<List<Capability>> findPage(int page, int size, CapabilitySortFieldEnum sortField, SortDirectionEnum direction) {
        long offset = (long) page * size;
        Flux<CapabilityEntity> entities = switch (sortField) {
            case NAME -> direction == SortDirectionEnum.DESC
                    ? repository.findPageByNameDesc(size, offset)
                    : repository.findPageByNameAsc(size, offset);
            case TECHNOLOGY_COUNT -> direction == SortDirectionEnum.DESC
                    ? repository.findPageByTechnologyCountDesc(size, offset)
                    : repository.findPageByTechnologyCountAsc(size, offset);
        };

        return entities.map(this::toEntity).collectList();
    }

    @Override
    public Mono<Long> count() {
        Query completeOnly = Query.query(Criteria.where(STATUS_COLUMN).is(CapabilityReactiveRepository.COMPLETE_STATUS));
        return template.count(completeOnly, CapabilityEntity.class);
    }
}
