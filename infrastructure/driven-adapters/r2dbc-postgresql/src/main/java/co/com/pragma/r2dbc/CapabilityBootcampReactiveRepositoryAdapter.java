package co.com.pragma.r2dbc;

import co.com.pragma.model.capability.exceptions.CapabilitiesNotFoundException;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.LinkBootcampCapabilities;
import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.exceptions.constant.FunctionalMessageConstants;
import co.com.pragma.r2dbc.entity.CapabilityBootcampEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Repository
public class CapabilityBootcampReactiveRepositoryAdapter extends
        ReactiveAdapterOperations<CapabilityBootcamp, CapabilityBootcampEntity, Long, CapabilityBootcampReactiveRepository>
        implements CapabilityBootcampRepository {

    private final CapabilityReactiveRepository capabilityRepository;

    public CapabilityBootcampReactiveRepositoryAdapter(CapabilityBootcampReactiveRepository repository,
            ObjectMapper mapper, CapabilityReactiveRepository capabilityRepository) {
        super(repository, mapper, d -> mapper.map(d, CapabilityBootcamp.class));
        this.capabilityRepository = capabilityRepository;
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
                .collectList()
                // la capacidad pudo haberse borrado físicamente (FK) entre el chequeo de
                // existencia previo y este INSERT; se mapea al mismo error de negocio que
                // ya usan los otros dos rechazos, en vez de dejar escapar el error técnico crudo
                .onErrorMap(DataIntegrityViolationException.class, ex -> new CapabilitiesNotFoundException(
                        FunctionalMessageConstants.BUSINESS_VALIDATION_FAILED,
                        Map.of(FieldConstants.CAPABILITY_IDS, String.format(FunctionalMessageConstants.CAPABILITIES_NOT_FOUND,
                                linkBootcampCapabilities.getCapabilityIds().value()))));
    }

    @Override
    public Mono<List<Long>> findDeletingCapabilityIds(LinkBootcampCapabilities linkBootcampCapabilities) {
        return repository.findDeletingCapabilityIds(linkBootcampCapabilities.getBootcampId().value(),
                linkBootcampCapabilities.getCapabilityIds().value()).collectList();
    }

    @Override
    public Mono<Void> deleteByBootcampId(Long bootcampId) {
        return repository.deleteByBootcampId(bootcampId);
    }

    @Override
    public Mono<List<CapabilityBootcamp>> findByBootcampIds(List<Long> bootcampIds) {
        return repository.findByBootcampIdIn(bootcampIds)
                .map(this::toEntity)
                .collectList();
    }

    @Override
    public Mono<List<Long>> findCapabilityIdsByBootcampId(Long bootcampId) {
        return repository.findCapabilityIdsByBootcampId(bootcampId).collectList();
    }

    @Override
    public Mono<List<Long>> findCapabilityIdsReferencedByOtherBootcamps(List<Long> capabilityIds,
            Long excludingBootcampId) {
        return repository.findCapabilityIdsReferencedByOtherBootcamps(capabilityIds, excludingBootcampId)
                .collectList();
    }

    @Override
    @Transactional
    public Mono<Void> deleteLinksAndOrphanedCapabilities(Long bootcampId, List<Long> candidateCapabilityIds) {
        return repository.deleteByBootcampId(bootcampId)
                .then(candidateCapabilityIds.isEmpty()
                        ? Mono.empty()
                        : capabilityRepository.deleteOrphaned(candidateCapabilityIds).then());
    }
}
