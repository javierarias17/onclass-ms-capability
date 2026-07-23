package co.com.pragma.usecase.checkcapabilitiesexistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CheckCapabilitiesExistenceUseCase {

    private final CapabilityRepository capabilityRepository;

    public Mono<List<Long>> execute(List<Long> capabilityIds) {
        Map<String, String> errors = collectFieldFormatErrors(capabilityIds);

        if (!errors.isEmpty())
            return Mono.error(new FieldsValidationException(errors));

        return capabilityRepository.findMissingIds(capabilityIds);
    }

    private Map<String, String> collectFieldFormatErrors(List<Long> capabilityIds) {
        Map<String, String> errors = new LinkedHashMap<>();
        FieldValidator.validateNotEmpty(capabilityIds, FieldConstants.CAPABILITY_IDS,
                ValidationMessageConstants.MSG_CAPABILITY_IDS_REQUIRED, errors);
        return errors;
    }
}
