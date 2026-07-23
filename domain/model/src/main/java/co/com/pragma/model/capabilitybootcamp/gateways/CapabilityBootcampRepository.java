package co.com.pragma.model.capabilitybootcamp.gateways;

import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.LinkBootcampCapabilities;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CapabilityBootcampRepository {
    Mono<List<CapabilityBootcamp>> saveAll(LinkBootcampCapabilities linkBootcampCapabilities);
}
