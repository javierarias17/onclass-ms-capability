package co.com.pragma.usecase.findcapabilitiesbybootcampids;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.capability.query.TechnologySummary;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindCapabilitiesByBootcampIdsUseCaseTest {

    private static final Long BOOTCAMP_ID = 10L;
    private static final Long OTHER_BOOTCAMP_ID = 11L;
    private static final Long CAPABILITY_ID = 1L;
    private static final Long OTHER_CAPABILITY_ID = 2L;
    private static final Long TECHNOLOGY_ID = 100L;
    private static final String CAPABILITY_NAME = "Backend";
    private static final String OTHER_CAPABILITY_NAME = "DevOps";
    private static final String TECHNOLOGY_NAME = "Java";
    private static final String VALID_DESCRIPTION = "Backend development capability";

    @Mock
    private CapabilityBootcampRepository capabilityBootcampRepository;

    @Mock
    private CapabilityRepository capabilityRepository;

    @Mock
    private TechnologyGateway technologyGateway;

    private FindCapabilitiesByBootcampIdsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindCapabilitiesByBootcampIdsUseCase(capabilityBootcampRepository, capabilityRepository, technologyGateway);
    }

    @Test
    void When_BootcampsHaveCapabilities_Expect_CapabilitiesGroupedByBootcampWithTechnologies() {
        // Arrange
        List<CapabilityBootcamp> relations = List.of(
                CapabilityBootcamp.builder().bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build(),
                CapabilityBootcamp.builder().bootcampId(BOOTCAMP_ID).capabilityId(OTHER_CAPABILITY_ID).build(),
                CapabilityBootcamp.builder().bootcampId(OTHER_BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build());
        List<Capability> capabilities = List.of(
                Capability.builder().id(CAPABILITY_ID).name(CAPABILITY_NAME).description(VALID_DESCRIPTION).build(),
                Capability.builder().id(OTHER_CAPABILITY_ID).name(OTHER_CAPABILITY_NAME).description(VALID_DESCRIPTION).build());
        Map<Long, List<TechnologySummary>> technologies = Map.of(CAPABILITY_ID,
                List.of(new TechnologySummary(TECHNOLOGY_ID, TECHNOLOGY_NAME)));

        when(capabilityBootcampRepository.findByBootcampIds(List.of(BOOTCAMP_ID, OTHER_BOOTCAMP_ID)))
                .thenReturn(Mono.just(relations));
        when(capabilityRepository.findByIds(List.of(CAPABILITY_ID, OTHER_CAPABILITY_ID)))
                .thenReturn(Mono.just(capabilities));
        when(technologyGateway.findTechnologiesByCapabilityIds(List.of(CAPABILITY_ID, OTHER_CAPABILITY_ID)))
                .thenReturn(Mono.just(technologies));

        // Act & Assert
        StepVerifier.create(useCase.execute(List.of(BOOTCAMP_ID, OTHER_BOOTCAMP_ID)))
                .expectNextMatches(result -> result.get(BOOTCAMP_ID).size() == 2
                        && result.get(OTHER_BOOTCAMP_ID).size() == 1
                        && result.get(OTHER_BOOTCAMP_ID).get(0).technologies().size() == 1)
                .verifyComplete();
    }

    @Test
    void When_NoRelationsFound_Expect_EmptyMapWithoutCallingOtherGateways() {
        // Arrange
        when(capabilityBootcampRepository.findByBootcampIds(List.of(BOOTCAMP_ID)))
                .thenReturn(Mono.just(List.of()));

        // Act & Assert
        StepVerifier.create(useCase.execute(List.of(BOOTCAMP_ID)))
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();

        verify(capabilityRepository, never()).findByIds(any());
        verify(technologyGateway, never()).findTechnologiesByCapabilityIds(any());
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdsIsNull() {
        // Act & Assert
        StepVerifier.create(useCase.execute(null))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).findByBootcampIds(any());
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdsIsEmpty() {
        // Act & Assert
        StepVerifier.create(useCase.execute(List.of()))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).findByBootcampIds(any());
    }

    @Test
    void Expect_ErrorToPropagate_When_TechnologyGatewayFails() {
        // Arrange
        List<CapabilityBootcamp> relations = List.of(
                CapabilityBootcamp.builder().bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build());
        List<Capability> capabilities = List.of(
                Capability.builder().id(CAPABILITY_ID).name(CAPABILITY_NAME).description(VALID_DESCRIPTION).build());
        RuntimeException failure = new RuntimeException("technology service unavailable");

        when(capabilityBootcampRepository.findByBootcampIds(List.of(BOOTCAMP_ID)))
                .thenReturn(Mono.just(relations));
        when(capabilityRepository.findByIds(List.of(CAPABILITY_ID)))
                .thenReturn(Mono.just(capabilities));
        when(technologyGateway.findTechnologiesByCapabilityIds(List.of(CAPABILITY_ID)))
                .thenReturn(Mono.error(failure));

        // Act & Assert
        StepVerifier.create(useCase.execute(List.of(BOOTCAMP_ID)))
                .expectErrorMatches(error -> error == failure)
                .verify();
    }
}
