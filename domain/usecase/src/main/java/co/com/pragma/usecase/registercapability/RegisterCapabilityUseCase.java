package co.com.pragma.usecase.registercapability;

import java.util.LinkedHashMap;
import java.util.Map;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.command.CapabilityCreateCommand;
import co.com.pragma.model.capability.CapabilityStatusEnum;
import co.com.pragma.model.capability.exceptions.CapabilityAlreadyExistsException;
import co.com.pragma.model.capability.exceptions.TechnologiesNotFoundException;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.capability.valueobject.CapabilityDescription;
import co.com.pragma.model.capability.valueobject.CapabilityName;
import co.com.pragma.model.capability.valueobject.CapabilityTechnologyIds;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.exceptions.FieldsValidationException;
import co.com.pragma.model.exceptions.constant.FunctionalMessageConstants;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RegisterCapabilityUseCase {

    private static final int NO_TECHNOLOGIES_LINKED_YET = 0;

    private final CapabilityRepository capabilityRepository;
    private final TechnologyGateway technologyGateway;

    public Mono<Capability> execute(CapabilityCreateCommand command) {
        Map<String, String> errors = collectFieldFormatErrors(command);

        if (!errors.isEmpty())
            return Mono.error(new FieldsValidationException(errors));

        return capabilityRepository.findByName(command.name())
                .flatMap(existingCapability -> resumeOrReject(existingCapability, command))
                .switchIfEmpty(Mono.defer(() -> registerAndLink(null, null, command)));
    }

    private Mono<Capability> resumeOrReject(Capability existingCapability, CapabilityCreateCommand command) {
        if (existingCapability.getStatus() == CapabilityStatusEnum.COMPLETE)
            return Mono.error(new CapabilityAlreadyExistsException(
                    FunctionalMessageConstants.BUSINESS_VALIDATION_FAILED,
                    Map.of(FieldConstants.NAME, FunctionalMessageConstants.CAPABILITY_ALREADY_EXISTS)));

        return registerAndLink(existingCapability.getId(), existingCapability.getVersion(), command);
    }

    private Mono<Capability> registerAndLink(Long capabilityId, Long version, CapabilityCreateCommand command) {
        return technologyGateway.checkTechnologiesExistence(command.technologyIds())
                .flatMap(missingIds -> missingIds.isEmpty()
                        ? capabilityRepository.save(Capability.builder()
                                .id(capabilityId)
                                .version(version)
                                .name(command.name())
                                .description(command.description())
                                .status(CapabilityStatusEnum.PENDING)
                                .technologyCount(NO_TECHNOLOGIES_LINKED_YET)
                                .build())
                        : Mono.error(new TechnologiesNotFoundException(
                                FunctionalMessageConstants.BUSINESS_VALIDATION_FAILED,
                                Map.of(FieldConstants.TECHNOLOGY_IDS,
                                        String.format(FunctionalMessageConstants.TECHNOLOGIES_NOT_FOUND, missingIds)))))
                .flatMap(savedCapability -> deleteStaleLinksIfResuming(capabilityId, savedCapability.getId())
                        .then(Mono.defer(() -> technologyGateway.linkCapabilityTechnologies(savedCapability.getId(), command.technologyIds())))
                        .then(Mono.defer(() -> capabilityRepository.save(Capability.builder()
                                .id(savedCapability.getId())
                                .version(savedCapability.getVersion())
                                .name(savedCapability.getName().value())
                                .description(savedCapability.getDescription().value())
                                .status(CapabilityStatusEnum.COMPLETE)
                                .technologyCount(command.technologyIds().size())
                                .build()))));
    }

    private Mono<Void> deleteStaleLinksIfResuming(Long existingCapabilityId, Long savedCapabilityId) {
        return existingCapabilityId != null
                ? technologyGateway.deleteCapabilityTechnologies(savedCapabilityId)
                : Mono.empty();
    }

    private Map<String, String> collectFieldFormatErrors(CapabilityCreateCommand command) {
        Map<String, String> errors = new LinkedHashMap<>();
        CapabilityName.validate(command.name(), errors);
        CapabilityDescription.validate(command.description(), errors);
        CapabilityTechnologyIds.validate(command.technologyIds(), errors);
        return errors;
    }
}
