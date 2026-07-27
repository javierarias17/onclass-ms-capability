package co.com.pragma.model.capability.gateways;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.query.CapabilitySortFieldEnum;
import co.com.pragma.model.capability.query.SortDirectionEnum;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CapabilityRepository {

    Mono<Capability> save(Capability capability);

    Mono<Capability> findByName(String name);

    Mono<List<Capability>> findPage(int page, int size, CapabilitySortFieldEnum sortField, SortDirectionEnum direction);

    Mono<Long> count();

    Mono<List<Long>> findMissingIds(List<Long> capabilityIds);

    Mono<List<Capability>> findByIds(List<Long> capabilityIds);

    Mono<Void> markAsDeleting(List<Long> capabilityIds);
}
