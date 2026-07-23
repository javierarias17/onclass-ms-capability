package co.com.pragma.usecase.deletebootcampcapabilities;

import java.util.LinkedHashMap;
import java.util.Map;

import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DeleteBootcampCapabilitiesUseCase {

    private final CapabilityBootcampRepository capabilityBootcampRepository;

    public Mono<Void> execute(String bootcampId) {
        Map<String, String> errors = collectFieldFormatErrors(bootcampId);

        if (!errors.isEmpty())
            return Mono.error(new FieldsValidationException(errors));

        return capabilityBootcampRepository.deleteByBootcampId(Long.valueOf(bootcampId));
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
