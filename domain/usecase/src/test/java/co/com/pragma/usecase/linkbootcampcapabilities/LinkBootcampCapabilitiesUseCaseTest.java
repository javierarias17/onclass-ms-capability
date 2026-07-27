package co.com.pragma.usecase.linkbootcampcapabilities;

import co.com.pragma.model.capability.exceptions.CapabilitiesNotFoundException;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.LinkBootcampCapabilities;
import co.com.pragma.model.capabilitybootcamp.command.LinkBootcampCapabilitiesCommand;
import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.model.exceptions.FieldsValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkBootcampCapabilitiesUseCaseTest {

        private static final Long LINK_ID = 1L;
        private static final Long BOOTCAMP_ID = 10L;
        private static final Long CAPABILITY_ID_1 = 1L;
        private static final Long CAPABILITY_ID_2 = 2L;
        private static final Long CAPABILITY_ID_3 = 3L;
        private static final Long CAPABILITY_ID_4 = 4L;
        private static final Long MISSING_CAPABILITY_ID = 99L;

        @Mock
        private CapabilityRepository capabilityRepository;

        @Mock
        private CapabilityBootcampRepository capabilityBootcampRepository;

        private LinkBootcampCapabilitiesUseCase useCase;

        @BeforeEach
        void setUp() {
                useCase = new LinkBootcampCapabilitiesUseCase(capabilityRepository, capabilityBootcampRepository);
        }

        @Test
        void When_AllCapabilitiesExist_Expect_LinksToBeSaved() {
                // Arrange
                LinkBootcampCapabilitiesCommand command = new LinkBootcampCapabilitiesCommand(BOOTCAMP_ID,
                                List.of(CAPABILITY_ID_1, CAPABILITY_ID_2, CAPABILITY_ID_3));
                List<CapabilityBootcamp> savedLinks = List.of(
                                CapabilityBootcamp.builder().id(LINK_ID).bootcampId(BOOTCAMP_ID)
                                                .capabilityId(CAPABILITY_ID_1).build());

                when(capabilityRepository.findMissingIds(command.capabilityIds())).thenReturn(Mono.just(List.of()));
                when(capabilityBootcampRepository.saveAll(any(LinkBootcampCapabilities.class)))
                                .thenReturn(Mono.just(savedLinks));
                when(capabilityBootcampRepository.findDeletingCapabilityIds(any(LinkBootcampCapabilities.class)))
                                .thenReturn(Mono.just(List.of()));

                // Act & Assert
                StepVerifier.create(useCase.execute(command))
                                .expectNextMatches(result -> result.equals(savedLinks))
                                .verifyComplete();

                ArgumentCaptor<LinkBootcampCapabilities> captor = ArgumentCaptor
                                .forClass(LinkBootcampCapabilities.class);
                verify(capabilityBootcampRepository).saveAll(captor.capture());
                LinkBootcampCapabilities saved = captor.getValue();
                assertEquals(BOOTCAMP_ID, saved.getBootcampId().value());
                assertEquals(command.capabilityIds(), saved.getCapabilityIds().value());
                verify(capabilityBootcampRepository, never()).deleteByBootcampId(any());
        }

        @Test
        void Expect_CapabilitiesNotFoundException_When_SomeSavedCapabilityEndedUpDeleting() {
                // Arrange: entre el chequeo de existencia y el guardado, otro request marcó
                // una de las capacidades como DELETING -> se deben borrar todas las
                // asociaciones recién creadas para este bootcamp y reportar el error con
                // todas las capacidades afectadas, no solo la primera.
                LinkBootcampCapabilitiesCommand command = new LinkBootcampCapabilitiesCommand(BOOTCAMP_ID,
                                List.of(CAPABILITY_ID_1, CAPABILITY_ID_2));
                List<CapabilityBootcamp> savedLinks = List.of(
                                CapabilityBootcamp.builder().id(LINK_ID).bootcampId(BOOTCAMP_ID)
                                                .capabilityId(CAPABILITY_ID_1).build());

                when(capabilityRepository.findMissingIds(command.capabilityIds())).thenReturn(Mono.just(List.of()));
                when(capabilityBootcampRepository.saveAll(any(LinkBootcampCapabilities.class)))
                                .thenReturn(Mono.just(savedLinks));
                when(capabilityBootcampRepository.findDeletingCapabilityIds(any(LinkBootcampCapabilities.class)))
                                .thenReturn(Mono.just(List.of(CAPABILITY_ID_2)));
                when(capabilityBootcampRepository.deleteByBootcampId(BOOTCAMP_ID)).thenReturn(Mono.empty());

                // Act & Assert
                StepVerifier.create(useCase.execute(command))
                                .expectError(CapabilitiesNotFoundException.class)
                                .verify();

                verify(capabilityBootcampRepository).deleteByBootcampId(BOOTCAMP_ID);
        }

        @Test
        void Expect_CapabilitiesNotFoundException_When_SomeCapabilitiesDoNotExist() {
                // Arrange
                LinkBootcampCapabilitiesCommand command = new LinkBootcampCapabilitiesCommand(BOOTCAMP_ID,
                                List.of(CAPABILITY_ID_1, MISSING_CAPABILITY_ID));

                when(capabilityRepository.findMissingIds(command.capabilityIds()))
                                .thenReturn(Mono.just(List.of(MISSING_CAPABILITY_ID)));

                // Act & Assert
                StepVerifier.create(useCase.execute(command))
                                .expectError(CapabilitiesNotFoundException.class)
                                .verify();

                verify(capabilityBootcampRepository, never()).saveAll(any(LinkBootcampCapabilities.class));
        }

        @Test
        void Expect_FieldsValidationException_When_BootcampIdIsNull() {
                // Arrange
                LinkBootcampCapabilitiesCommand command = new LinkBootcampCapabilitiesCommand(null,
                                List.of(CAPABILITY_ID_1, CAPABILITY_ID_2));

                // Act & Assert
                StepVerifier.create(useCase.execute(command))
                                .expectError(FieldsValidationException.class)
                                .verify();

                verify(capabilityRepository, never()).findMissingIds(anyList());
        }

        @Test
        void Expect_FieldsValidationException_When_CapabilityIdsIsEmpty() {
                // Arrange
                LinkBootcampCapabilitiesCommand command = new LinkBootcampCapabilitiesCommand(BOOTCAMP_ID, List.of());

                // Act & Assert
                StepVerifier.create(useCase.execute(command))
                                .expectError(FieldsValidationException.class)
                                .verify();

                verify(capabilityRepository, never()).findMissingIds(anyList());
        }

        @Test
        void Expect_FieldsValidationException_When_CapabilityIdsExceedsMaxSize() {
                // Arrange
                LinkBootcampCapabilitiesCommand command = new LinkBootcampCapabilitiesCommand(BOOTCAMP_ID,
                                List.of(CAPABILITY_ID_1, CAPABILITY_ID_2, CAPABILITY_ID_3, CAPABILITY_ID_4,
                                                MISSING_CAPABILITY_ID));

                // Act & Assert
                StepVerifier.create(useCase.execute(command))
                                .expectError(FieldsValidationException.class)
                                .verify();

                verify(capabilityRepository, never()).findMissingIds(anyList());
        }

        @Test
        void Expect_FieldsValidationException_When_CapabilityIdsHasDuplicates() {
                // Arrange
                LinkBootcampCapabilitiesCommand command = new LinkBootcampCapabilitiesCommand(BOOTCAMP_ID,
                                List.of(CAPABILITY_ID_1, CAPABILITY_ID_1));

                // Act & Assert
                StepVerifier.create(useCase.execute(command))
                                .expectError(FieldsValidationException.class)
                                .verify();

                verify(capabilityRepository, never()).findMissingIds(anyList());
        }
}
