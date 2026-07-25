package co.com.pragma.usecase.listcapabilities;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.query.CapabilityListQuery;
import co.com.pragma.model.capability.query.CapabilitySortFieldEnum;
import co.com.pragma.model.capability.query.SortDirectionEnum;
import co.com.pragma.model.capability.query.TechnologySummary;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.exceptions.FieldsValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCapabilitiesUseCaseTest {

    private static final String VALID_NAME = "Backend";
    private static final String VALID_DESCRIPTION = "Backend development capability";
    private static final Long CAPABILITY_ID = 1L;
    private static final Long TECHNOLOGY_ID = 10L;
    private static final String TECHNOLOGY_NAME = "Java";
    private static final long ONE_ELEMENT = 1L;
    private static final long ZERO_ELEMENTS = 0L;

    private static final String PAGE = "0";
    private static final String SIZE = "10";
    private static final int PAGE_VALUE = 0;
    private static final int SIZE_VALUE = 10;
    private static final String SORT_BY_NAME = "NAME";
    private static final String SORT_BY_TECHNOLOGY_COUNT = "TECHNOLOGY_COUNT";
    private static final String SORT_DIRECTION_ASC = "ASC";
    private static final String SORT_DIRECTION_DESC = "DESC";

    private static final String BLANK_VALUE = " ";
    private static final String NOT_NUMERIC_VALUE = "abc";
    private static final String NEGATIVE_PAGE = "-1";
    private static final String OVERFLOWING_PAGE = "99999999999";
    private static final String ZERO_SIZE = "0";
    private static final String SIZE_EXCEEDING_MAX = "101";
    private static final String INVALID_SORT_FIELD = "invalidField";
    private static final String INVALID_SORT_DIRECTION = "invalidDirection";

    @Mock
    private CapabilityRepository capabilityRepository;

    @Mock
    private TechnologyGateway technologyGateway;

    private ListCapabilitiesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListCapabilitiesUseCase(capabilityRepository, technologyGateway);
    }

    @Test
    void When_CapabilitiesExist_Expect_PageEnrichedWithTechnologies() {
        // Arrange
        Capability capability = Capability.builder().id(CAPABILITY_ID).name(VALID_NAME).description(VALID_DESCRIPTION).build();
        CapabilityListQuery query = new CapabilityListQuery(PAGE, SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);
        Map<Long, List<TechnologySummary>> technologies = Map.of(CAPABILITY_ID, List.of(new TechnologySummary(TECHNOLOGY_ID, TECHNOLOGY_NAME)));

        when(capabilityRepository.findPage(PAGE_VALUE, SIZE_VALUE, CapabilitySortFieldEnum.NAME, SortDirectionEnum.ASC))
                .thenReturn(Mono.just(List.of(capability)));
        when(capabilityRepository.count()).thenReturn(Mono.just(ONE_ELEMENT));
        when(technologyGateway.findTechnologiesByCapabilityIds(List.of(CAPABILITY_ID)))
                .thenReturn(Mono.just(technologies));

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectNextMatches(page -> page.content().size() == 1
                        && page.content().get(0).technologies().size() == 1
                        && page.totalElements() == ONE_ELEMENT
                        && page.totalPages() == 1)
                .verifyComplete();
    }

    @Test
    void When_NoCapabilitiesExist_Expect_EmptyPageWithoutCallingTechnologyGateway() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        when(capabilityRepository.findPage(PAGE_VALUE, SIZE_VALUE, CapabilitySortFieldEnum.NAME, SortDirectionEnum.ASC))
                .thenReturn(Mono.just(List.of()));
        when(capabilityRepository.count()).thenReturn(Mono.just(ZERO_ELEMENTS));

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectNextMatches(page -> page.content().isEmpty() && page.totalElements() == ZERO_ELEMENTS)
                .verifyComplete();

        verify(technologyGateway, never()).findTechnologiesByCapabilityIds(any());
    }

    @Test
    void When_SortByTechnologyCountDesc_Expect_RepositoryCalledWithMappedSort() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, SIZE, SORT_BY_TECHNOLOGY_COUNT, SORT_DIRECTION_DESC);

        when(capabilityRepository.findPage(PAGE_VALUE, SIZE_VALUE, CapabilitySortFieldEnum.TECHNOLOGY_COUNT, SortDirectionEnum.DESC))
                .thenReturn(Mono.just(List.of()));
        when(capabilityRepository.count()).thenReturn(Mono.just(ZERO_ELEMENTS));

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void Expect_ErrorToPropagate_When_TechnologyGatewayFails() {
        // Arrange
        Capability capability = Capability.builder().id(CAPABILITY_ID).name(VALID_NAME).description(VALID_DESCRIPTION).build();
        CapabilityListQuery query = new CapabilityListQuery(PAGE, SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);
        RuntimeException failure = new RuntimeException("technology service unavailable");

        when(capabilityRepository.findPage(PAGE_VALUE, SIZE_VALUE, CapabilitySortFieldEnum.NAME, SortDirectionEnum.ASC))
                .thenReturn(Mono.just(List.of(capability)));
        when(capabilityRepository.count()).thenReturn(Mono.just(ONE_ELEMENT));
        when(technologyGateway.findTechnologiesByCapabilityIds(List.of(CAPABILITY_ID)))
                .thenReturn(Mono.error(failure));

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectErrorMatches(error -> error == failure)
                .verify();
    }

    @Test
    void Expect_FieldsValidationException_When_PageIsNotNumeric() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(NOT_NUMERIC_VALUE, SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_PageIsNegative() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(NEGATIVE_PAGE, SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_PageOverflowsIntegerRange() {
        // Arrange: numérico según el regex, pero no cabe en un int
        CapabilityListQuery query = new CapabilityListQuery(OVERFLOWING_PAGE, SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_PageIsBlank() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(BLANK_VALUE, SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_SizeIsBlank() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, BLANK_VALUE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_SizeIsNotNumeric() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, NOT_NUMERIC_VALUE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_SizeIsOutOfRange() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, ZERO_SIZE, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_SizeExceedsMax() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, SIZE_EXCEEDING_MAX, SORT_BY_NAME, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_SortByIsInvalid() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, SIZE, INVALID_SORT_FIELD, SORT_DIRECTION_ASC);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_SortDirectionIsInvalid() {
        // Arrange
        CapabilityListQuery query = new CapabilityListQuery(PAGE, SIZE, SORT_BY_NAME, INVALID_SORT_DIRECTION);

        // Act & Assert
        StepVerifier.create(useCase.execute(query))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findPage(anyInt(), anyInt(), any(), any());
    }
}
