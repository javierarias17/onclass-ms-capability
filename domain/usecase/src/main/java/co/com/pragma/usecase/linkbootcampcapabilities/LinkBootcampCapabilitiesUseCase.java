package co.com.pragma.usecase.linkbootcampcapabilities;

import java.util.List;
import java.util.Map;

import co.com.pragma.model.capability.exceptions.CapabilitiesNotFoundException;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.LinkBootcampCapabilities;
import co.com.pragma.model.capabilitybootcamp.command.LinkBootcampCapabilitiesCommand;
import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.exceptions.constant.FunctionalMessageConstants;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class LinkBootcampCapabilitiesUseCase {

    private final CapabilityRepository capabilityRepository;
    private final CapabilityBootcampRepository capabilityBootcampRepository;

    public Mono<List<CapabilityBootcamp>> execute(LinkBootcampCapabilitiesCommand command) {
        return Mono.fromCallable(() -> LinkBootcampCapabilities.builder()
                        .bootcampId(command.bootcampId())
                        .capabilityIds(command.capabilityIds())
                        .build())
                .flatMap(request -> capabilityRepository.findMissingIds(request.getCapabilityIds().value())
                        .flatMap(missingIds -> missingIds.isEmpty()
                                ? capabilityBootcampRepository.saveAll(request)
                                : Mono.error(new CapabilitiesNotFoundException(
                                        FunctionalMessageConstants.BUSINESS_VALIDATION_FAILED,
                                        Map.of(FieldConstants.CAPABILITY_IDS,
                                                String.format(FunctionalMessageConstants.CAPABILITIES_NOT_FOUND, missingIds))))));
    }
}
