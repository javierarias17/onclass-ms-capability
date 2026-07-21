package co.com.pragma.model.capability.gateways;

import reactor.core.publisher.Mono;

import java.util.List;

public interface TechnologyGateway {

    Mono<List<Long>> checkTechnologiesExistence(List<Long> technologyIds);

    Mono<Void> linkCapabilityTechnologies(Long capabilityId, List<Long> technologyIds);

    Mono<Void> deleteCapabilityTechnologies(Long capabilityId);
}
