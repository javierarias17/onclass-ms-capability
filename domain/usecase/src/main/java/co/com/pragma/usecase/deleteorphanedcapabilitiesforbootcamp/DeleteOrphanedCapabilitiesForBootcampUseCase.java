package co.com.pragma.usecase.deleteorphanedcapabilitiesforbootcamp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DeleteOrphanedCapabilitiesForBootcampUseCase {

    private final CapabilityBootcampRepository capabilityBootcampRepository;
    private final CapabilityRepository capabilityRepository;
    private final TechnologyGateway technologyGateway;

    public Mono<Void> execute(String bootcampId) {
        Map<String, String> errors = collectFieldFormatErrors(bootcampId);

        if (!errors.isEmpty())
            return Mono.error(new FieldsValidationException(errors));

        return capabilityBootcampRepository.findCapabilityIdsByBootcampId(Long.valueOf(bootcampId))
                .flatMap(linkedCapabilityIds -> linkedCapabilityIds.isEmpty()
                        ? Mono.empty()
                        : deleteOrphanedCapabilities(Long.valueOf(bootcampId), linkedCapabilityIds));
    }

    private Mono<Void> deleteOrphanedCapabilities(Long bootcampId, List<Long> linkedCapabilityIds) {
        return capabilityBootcampRepository.findCapabilityIdsReferencedByOtherBootcamps(linkedCapabilityIds, bootcampId)
                .map(survivingCapabilityIds -> linkedCapabilityIds.stream()
                        .filter(id -> !survivingCapabilityIds.contains(id)).toList())
                .flatMap(candidateCapabilityIds -> markAsDeletingIfAny(candidateCapabilityIds)
                        .then(Mono.defer(() -> deleteOrphanedTechnologiesIfAny(candidateCapabilityIds)))
                        .then(Mono.defer(() -> capabilityBootcampRepository
                                .deleteLinksAndOrphanedCapabilities(bootcampId, candidateCapabilityIds))));
    }

    private Mono<Void> markAsDeletingIfAny(List<Long> candidateCapabilityIds) {
        if (candidateCapabilityIds.isEmpty())
            return Mono.empty();
        return capabilityRepository.markAsDeleting(candidateCapabilityIds);
    }

    private Mono<Void> deleteOrphanedTechnologiesIfAny(List<Long> candidateCapabilityIds) {
        if (candidateCapabilityIds.isEmpty())
            return Mono.empty();
        return technologyGateway.deleteOrphanedTechnologiesForCapabilities(candidateCapabilityIds);
    }

    private Map<String, String> collectFieldFormatErrors(String bootcampId) {
        Map<String, String> errors = new LinkedHashMap<>();
        FieldValidator.validateNotBlank(bootcampId, FieldConstants.BOOTCAMP_ID,
                ValidationMessageConstants.MSG_BOOTCAMP_ID_REQUIRED, errors);
        FieldValidator.validateNumericFormat(bootcampId, FieldConstants.BOOTCAMP_ID,
                ValidationMessageConstants.MSG_BOOTCAMP_ID_MUST_BE_NUMERIC, errors);
        return errors;
    }
}
