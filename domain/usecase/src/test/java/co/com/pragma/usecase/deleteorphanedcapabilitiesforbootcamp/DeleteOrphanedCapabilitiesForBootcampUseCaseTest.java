package co.com.pragma.usecase.deleteorphanedcapabilitiesforbootcamp;

import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteOrphanedCapabilitiesForBootcampUseCaseTest {

    private static final String BOOTCAMP_ID = "10";
    private static final Long BOOTCAMP_ID_VALUE = 10L;
    private static final Long CAPABILITY_ID = 1L;
    private static final Long OTHER_CAPABILITY_ID = 2L;
    private static final String BLANK_BOOTCAMP_ID = "  ";
    private static final String NOT_NUMERIC_BOOTCAMP_ID = "1022-";

    @Mock
    private CapabilityBootcampRepository capabilityBootcampRepository;

    @Mock
    private CapabilityRepository capabilityRepository;

    @Mock
    private TechnologyGateway technologyGateway;

    private DeleteOrphanedCapabilitiesForBootcampUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteOrphanedCapabilitiesForBootcampUseCase(
                capabilityBootcampRepository, capabilityRepository, technologyGateway);
    }

    @Test
    void When_BootcampHasNoLinkedCapabilities_Expect_NothingElseToBeCalled() {
        // Arrange: ya se habían borrado (o nunca hubo), no hay nada que hacer
        when(capabilityBootcampRepository.findCapabilityIdsByBootcampId(BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of()));

        // Act & Assert
        StepVerifier.create(useCase.execute(BOOTCAMP_ID))
                .verifyComplete();

        verify(technologyGateway, never()).deleteOrphanedTechnologiesForCapabilities(any());
        verify(capabilityBootcampRepository, never()).deleteLinksAndOrphanedCapabilities(anyLong(), any());
    }

    @Test
    void When_AllLinkedCapabilitiesSurviveInOtherBootcamps_Expect_OnlyLinksToBeDeleted() {
        // Arrange: la capacidad sigue en uso por otro bootcamp, no debe borrarse
        when(capabilityBootcampRepository.findCapabilityIdsByBootcampId(BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of(CAPABILITY_ID)));
        when(capabilityBootcampRepository.findCapabilityIdsReferencedByOtherBootcamps(List.of(CAPABILITY_ID), BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of(CAPABILITY_ID)));
        when(capabilityBootcampRepository.deleteLinksAndOrphanedCapabilities(BOOTCAMP_ID_VALUE, List.of()))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(useCase.execute(BOOTCAMP_ID))
                .verifyComplete();

        verify(capabilityRepository, never()).markAsDeleting(any());
        verify(technologyGateway, never()).deleteOrphanedTechnologiesForCapabilities(any());
        verify(capabilityBootcampRepository).deleteLinksAndOrphanedCapabilities(BOOTCAMP_ID_VALUE, List.of());
    }

    @Test
    void When_SomeCapabilitiesBecomeOrphaned_Expect_TechnologyCascadeThenLocalDelete() {
        // Arrange
        List<Long> linkedIds = List.of(CAPABILITY_ID, OTHER_CAPABILITY_ID);
        when(capabilityBootcampRepository.findCapabilityIdsByBootcampId(BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(linkedIds));
        // solo OTHER_CAPABILITY_ID sigue en uso por otro bootcamp; CAPABILITY_ID queda huérfana
        when(capabilityBootcampRepository.findCapabilityIdsReferencedByOtherBootcamps(linkedIds, BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of(OTHER_CAPABILITY_ID)));
        when(capabilityRepository.markAsDeleting(List.of(CAPABILITY_ID))).thenReturn(Mono.empty());
        when(technologyGateway.deleteOrphanedTechnologiesForCapabilities(List.of(CAPABILITY_ID)))
                .thenReturn(Mono.empty());
        when(capabilityBootcampRepository.deleteLinksAndOrphanedCapabilities(BOOTCAMP_ID_VALUE, List.of(CAPABILITY_ID)))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(useCase.execute(BOOTCAMP_ID))
                .verifyComplete();

        verify(capabilityRepository).markAsDeleting(List.of(CAPABILITY_ID));
        verify(technologyGateway).deleteOrphanedTechnologiesForCapabilities(List.of(CAPABILITY_ID));
        verify(capabilityBootcampRepository).deleteLinksAndOrphanedCapabilities(BOOTCAMP_ID_VALUE, List.of(CAPABILITY_ID));
    }

    @Test
    void Expect_TechnologyGatewayToBeSkipped_When_MarkAsDeletingFails() {
        // Arrange: si no se puede marcar la capacidad como DELETING, no debe avanzarse a
        // technology-ms ni al borrado local, para no dejar tecnologías huérfanas de un
        // borrado que en realidad no quedó registrado como en curso.
        RuntimeException failure = new RuntimeException("database unavailable");
        when(capabilityBootcampRepository.findCapabilityIdsByBootcampId(BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of(CAPABILITY_ID)));
        when(capabilityBootcampRepository.findCapabilityIdsReferencedByOtherBootcamps(List.of(CAPABILITY_ID), BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of()));
        when(capabilityRepository.markAsDeleting(List.of(CAPABILITY_ID))).thenReturn(Mono.error(failure));

        // Act & Assert
        StepVerifier.create(useCase.execute(BOOTCAMP_ID))
                .expectErrorMatches(error -> error == failure)
                .verify();

        verify(technologyGateway, never()).deleteOrphanedTechnologiesForCapabilities(any());
        verify(capabilityBootcampRepository, never()).deleteLinksAndOrphanedCapabilities(anyLong(), any());
    }

    @Test
    void Expect_LocalDeleteToBeSkipped_When_TechnologyGatewayFails() {
        // Arrange: si technology-ms falla, no se debe tocar nada localmente todavía
        // (para poder reintentar sin haber perdido la evidencia de qué falta hacer).
        RuntimeException failure = new RuntimeException("technology service unavailable");
        when(capabilityBootcampRepository.findCapabilityIdsByBootcampId(BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of(CAPABILITY_ID)));
        when(capabilityBootcampRepository.findCapabilityIdsReferencedByOtherBootcamps(List.of(CAPABILITY_ID), BOOTCAMP_ID_VALUE))
                .thenReturn(Mono.just(List.of()));
        when(capabilityRepository.markAsDeleting(List.of(CAPABILITY_ID))).thenReturn(Mono.empty());
        when(technologyGateway.deleteOrphanedTechnologiesForCapabilities(List.of(CAPABILITY_ID)))
                .thenReturn(Mono.error(failure));

        // Act & Assert
        StepVerifier.create(useCase.execute(BOOTCAMP_ID))
                .expectErrorMatches(error -> error == failure)
                .verify();

        verify(capabilityBootcampRepository, never()).deleteLinksAndOrphanedCapabilities(anyLong(), any());
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdIsNull() {
        // Act & Assert
        StepVerifier.create(useCase.execute(null))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).findCapabilityIdsByBootcampId(any());
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdIsBlank() {
        // Act & Assert
        StepVerifier.create(useCase.execute(BLANK_BOOTCAMP_ID))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).findCapabilityIdsByBootcampId(any());
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdIsNotNumeric() {
        // Act & Assert
        StepVerifier.create(useCase.execute(NOT_NUMERIC_BOOTCAMP_ID))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).findCapabilityIdsByBootcampId(any());
    }
}
