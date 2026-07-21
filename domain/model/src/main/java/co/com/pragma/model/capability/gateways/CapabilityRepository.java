package co.com.pragma.model.capability.gateways;

import co.com.pragma.model.capability.Capability;
import reactor.core.publisher.Mono;

public interface CapabilityRepository {

    Mono<Capability> save(Capability capability);

    Mono<Capability> findByName(String name);
}
