package co.com.pragma.usecase.listcapabilities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.query.CapabilityListItem;
import co.com.pragma.model.capability.query.CapabilityListQuery;
import co.com.pragma.model.capability.query.CapabilityPage;
import co.com.pragma.model.capability.query.CapabilitySortFieldEnum;
import co.com.pragma.model.capability.query.SortDirectionEnum;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.model.common.ValidationMessageConstants;
import co.com.pragma.model.common.validator.FieldValidator;
import co.com.pragma.model.exceptions.FieldsValidationException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ListCapabilitiesUseCase {

    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;
    private static final String SORT_BY_TECHNOLOGY_COUNT = "technologyCount";
    private static final String SORT_DIRECTION_DESC = "desc";
    private static final String SORT_BY_NAME= "name";
    private static final String SORT_DIRECTION_ASC = "asc";

    private final CapabilityRepository capabilityRepository;
    private final TechnologyGateway technologyGateway;

    public Mono<CapabilityPage> execute(CapabilityListQuery query) {
        Map<String, String> errors = collectFieldFormatErrors(query);

        if (!errors.isEmpty())
            return Mono.error(new FieldsValidationException(errors));

        return Mono.fromCallable(() -> buildValidatedParams(query))
                .flatMap(params -> Mono.zip(
                                capabilityRepository.findPage(params.page(), params.size(), params.sortField(), params.direction()),
                                capabilityRepository.count())
                        .flatMap(tuple -> buildCapabilityPage(tuple.getT1(), tuple.getT2(), params.page(), params.size())));
    }

    private ValidatedParams buildValidatedParams(CapabilityListQuery query) {
        return ValidatedParams.builder()
                .page(Integer.parseInt(query.page()))
                .size(Integer.parseInt(query.size()))
                .sortField(SORT_BY_TECHNOLOGY_COUNT.equals(query.sortBy())
                        ? CapabilitySortFieldEnum.TECHNOLOGY_COUNT
                        : CapabilitySortFieldEnum.NAME)
                .direction(SORT_DIRECTION_DESC.equals(query.sortDirection())
                        ? SortDirectionEnum.DESC
                        : SortDirectionEnum.ASC)
                .build();
    }

    private Mono<CapabilityPage> buildCapabilityPage(List<Capability> capabilities, long totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        if (capabilities.isEmpty())
            return Mono.just(new CapabilityPage(List.of(), page, size, totalElements, totalPages));

        List<Long> capabilityIds = capabilities.stream().map(Capability::getId).toList();

        return technologyGateway.findTechnologiesByCapabilityIds(capabilityIds)
                .map(technologiesByCapability -> capabilities.stream()
                        .map(capability -> new CapabilityListItem(capability,
                                technologiesByCapability.getOrDefault(capability.getId(), List.of())))
                        .toList())
                .map(content -> new CapabilityPage(content, page, size, totalElements, totalPages));
    }

    private Map<String, String> collectFieldFormatErrors(CapabilityListQuery query) {
        Map<String, String> errors = new LinkedHashMap<>();

        FieldValidator.validateNotBlank(query.page(), FieldConstants.PAGE,
                ValidationMessageConstants.MSG_PAGE_MUST_BE_NUMERIC, errors);
        FieldValidator.validateNumericFormat(query.page(), FieldConstants.PAGE,
                ValidationMessageConstants.MSG_PAGE_MUST_BE_NUMERIC, errors);
        if (!errors.containsKey(FieldConstants.PAGE))
            FieldValidator.validateIntegerRange(query.page(), MIN_PAGE, Integer.MAX_VALUE, FieldConstants.PAGE,
                    ValidationMessageConstants.MSG_PAGE_OUT_OF_RANGE, errors);

        FieldValidator.validateNotBlank(query.size(), FieldConstants.SIZE,
                ValidationMessageConstants.MSG_SIZE_MUST_BE_NUMERIC, errors);
        FieldValidator.validateNumericFormat(query.size(), FieldConstants.SIZE,
                ValidationMessageConstants.MSG_SIZE_MUST_BE_NUMERIC, errors);
        if (!errors.containsKey(FieldConstants.SIZE))
            FieldValidator.validateIntegerRange(query.size(), MIN_SIZE, MAX_SIZE, FieldConstants.SIZE,
                    String.format(ValidationMessageConstants.MSG_SIZE_OUT_OF_RANGE, MIN_SIZE, MAX_SIZE), errors);

        FieldValidator.validateAllowedValue(query.sortBy(), Set.of(SORT_BY_NAME, SORT_BY_TECHNOLOGY_COUNT), FieldConstants.SORT_BY,
                ValidationMessageConstants.MSG_SORT_BY_INVALID, errors);

        FieldValidator.validateAllowedValue(query.sortDirection(), Set.of(SORT_DIRECTION_ASC, SORT_DIRECTION_DESC), FieldConstants.SORT_DIRECTION,
                ValidationMessageConstants.MSG_SORT_DIRECTION_INVALID, errors);

        return errors;
    }

    @Builder
    private record ValidatedParams(int page, int size, CapabilitySortFieldEnum sortField, SortDirectionEnum direction) {
    }
}
