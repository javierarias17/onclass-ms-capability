package co.com.pragma.model.capabilitybootcamp.gateways;

import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.LinkBootcampCapabilities;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CapabilityBootcampRepository {
    Mono<List<CapabilityBootcamp>> saveAll(LinkBootcampCapabilities linkBootcampCapabilities);

    Mono<Void> deleteByBootcampId(Long bootcampId);

    Mono<List<CapabilityBootcamp>> findByBootcampIds(List<Long> bootcampIds);

    Mono<List<Long>> findCapabilityIdsByBootcampId(Long bootcampId);

    Mono<List<Long>> findCapabilityIdsReferencedByOtherBootcamps(List<Long> capabilityIds, Long excludingBootcampId);

    Mono<List<Long>> findDeletingCapabilityIds(LinkBootcampCapabilities linkBootcampCapabilities);

    Mono<Void> deleteLinksAndOrphanedCapabilities(Long bootcampId, List<Long> candidateCapabilityIds);
}
