package co.com.pragma.model.capability.gateways;

import co.com.pragma.model.capability.query.TechnologySummary;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface TechnologyGateway {

    Mono<List<Long>> checkTechnologiesExistence(List<Long> technologyIds);

    Mono<Void> linkCapabilityTechnologies(Long capabilityId, List<Long> technologyIds);

    Mono<Void> deleteCapabilityTechnologies(Long capabilityId);

    Mono<Map<Long, List<TechnologySummary>>> findTechnologiesByCapabilityIds(List<Long> capabilityIds);
}
