package co.com.pragma.r2dbc;

import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.LinkBootcampCapabilities;
import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.r2dbc.entity.CapabilityBootcampEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class CapabilityBootcampReactiveRepositoryAdapter extends
        ReactiveAdapterOperations<CapabilityBootcamp, CapabilityBootcampEntity, Long, CapabilityBootcampReactiveRepository>
        implements CapabilityBootcampRepository {

    public CapabilityBootcampReactiveRepositoryAdapter(CapabilityBootcampReactiveRepository repository,
            ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, CapabilityBootcamp.class));
    }

    @Override
    @Transactional
    public Mono<List<CapabilityBootcamp>> saveAll(LinkBootcampCapabilities linkBootcampCapabilities) {
        Long bootcampId = linkBootcampCapabilities.getBootcampId().value();
        return Flux.fromIterable(linkBootcampCapabilities.getCapabilityIds().value())
                .flatMap(capabilityId -> repository.insertIgnoringConflict(bootcampId, capabilityId)
                        .map(this::toEntity)
                        .defaultIfEmpty(CapabilityBootcamp.builder()
                                .bootcampId(bootcampId)
                                .capabilityId(capabilityId)
                                .build()))
                .collectList();
    }
}
