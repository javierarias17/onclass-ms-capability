package co.com.pragma.usecase.findcapabilitiesbybootcampids;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.capability.query.TechnologySummary;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.model.capabilitybootcamp.query.CapabilitySummary;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class FindCapabilitiesByBootcampIdsUseCase {

    private final CapabilityBootcampRepository capabilityBootcampRepository;
    private final CapabilityRepository capabilityRepository;
    private final TechnologyGateway technologyGateway;

    public Mono<Map<Long, List<CapabilitySummary>>> execute(List<Long> bootcampIds) {
        Map<String, String> errors = collectFieldFormatErrors(bootcampIds);

        if (!errors.isEmpty())
            return Mono.error(new FieldsValidationException(errors));

        return capabilityBootcampRepository.findByBootcampIds(bootcampIds)
                .flatMap(this::buildCapabilitiesByBootcamp);
    }

    private Map<String, String> collectFieldFormatErrors(List<Long> bootcampIds) {
        Map<String, String> errors = new LinkedHashMap<>();
        FieldValidator.validateNotEmpty(bootcampIds, FieldConstants.BOOTCAMP_IDS,
                ValidationMessageConstants.MSG_BOOTCAMP_IDS_REQUIRED, errors);
        return errors;
    }

    private Mono<Map<Long, List<CapabilitySummary>>> buildCapabilitiesByBootcamp(List<CapabilityBootcamp> relations) {
        if (relations.isEmpty())
            return Mono.just(Map.of());

        List<Long> capabilityIds = relations.stream()
                .map(CapabilityBootcamp::getCapabilityId)
                .distinct()
                .toList();

        return Mono.zip(
                        capabilityRepository.findByIds(capabilityIds),
                        technologyGateway.findTechnologiesByCapabilityIds(capabilityIds))
                .map(tuple -> toCapabilitySummaries(tuple.getT1(), tuple.getT2()))
                .map(capabilitySummaries -> groupByBootcampId(relations, capabilitySummaries));
    }

    private Map<Long, CapabilitySummary> toCapabilitySummaries(List<Capability> capabilities,
            Map<Long, List<TechnologySummary>> technologiesByCapability) {
        return capabilities.stream()
                .collect(Collectors.toMap(Capability::getId, capability -> new CapabilitySummary(
                        capability.getId(),
                        capability.getName().value(),
                        technologiesByCapability.getOrDefault(capability.getId(), List.of()))));
    }

    private Map<Long, List<CapabilitySummary>> groupByBootcampId(List<CapabilityBootcamp> relations,
            Map<Long, CapabilitySummary> capabilitySummariesById) {
        return relations.stream()
                .filter(relation -> capabilitySummariesById.containsKey(relation.getCapabilityId()))
                .collect(Collectors.groupingBy(
                        CapabilityBootcamp::getBootcampId,
                        Collectors.mapping(
                                relation -> capabilitySummariesById.get(relation.getCapabilityId()),
                                Collectors.toList())));
    }
}
