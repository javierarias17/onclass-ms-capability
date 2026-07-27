package co.com.pragma.usecase.registercapability;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.command.CapabilityCreateCommand;
import co.com.pragma.model.capability.CapabilityStatusEnum;
import co.com.pragma.model.capability.exceptions.CapabilityAlreadyExistsException;
import co.com.pragma.model.capability.exceptions.TechnologiesNotFoundException;
import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.capability.valueobject.CapabilityTechnologyIds;
import co.com.pragma.model.exceptions.FieldsValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.LongStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterCapabilityUseCaseTest {

    private static final String VALID_NAME = "Backend";
    private static final String VALID_DESCRIPTION = "Backend development capability";
    private static final Long CAPABILITY_ID = 10L;
    private static final Long TECHNOLOGY_ID_1 = 1L;
    private static final Long TECHNOLOGY_ID_2 = 2L;
    private static final Long TECHNOLOGY_ID_3 = 3L;
    private static final List<Long> VALID_TECHNOLOGY_IDS = List.of(TECHNOLOGY_ID_1, TECHNOLOGY_ID_2, TECHNOLOGY_ID_3);
    private static final List<Long> MISSING_TECHNOLOGY_IDS = List.of(TECHNOLOGY_ID_3);
    private static final String BLANK_VALUE = " ";

    @Mock
    private CapabilityRepository capabilityRepository;

    @Mock
    private TechnologyGateway technologyGateway;

    private RegisterCapabilityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterCapabilityUseCase(capabilityRepository, technologyGateway);
    }

    @Test
    void When_CapabilityInformationIsValid_Expect_CapabilityToBeSavedAndLinked() {
        // Arrange
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, VALID_TECHNOLOGY_IDS);
        Capability creatingCapability = capabilityWithStatus(CAPABILITY_ID, CapabilityStatusEnum.CREATING);
        Capability createdCapability = capabilityWithStatus(CAPABILITY_ID, CapabilityStatusEnum.CREATED);

        when(capabilityRepository.findByName(VALID_NAME)).thenReturn(Mono.empty());
        when(technologyGateway.checkTechnologiesExistence(VALID_TECHNOLOGY_IDS)).thenReturn(Mono.just(List.of()));
        when(capabilityRepository.save(any(Capability.class)))
                .thenReturn(Mono.just(creatingCapability), Mono.just(createdCapability));
        when(technologyGateway.linkCapabilityTechnologies(CAPABILITY_ID, VALID_TECHNOLOGY_IDS)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectNextMatches(result -> result.getId().equals(CAPABILITY_ID)
                        && result.getName().value().equals(VALID_NAME)
                        && result.getStatus() == CapabilityStatusEnum.CREATED)
                .verifyComplete();

        // registro nuevo: no hay vínculos previos que limpiar
        verify(technologyGateway, never()).deleteCapabilityTechnologies(anyLong());
        verify(capabilityRepository, times(2)).save(any(Capability.class));
    }

    @Test
    void Expect_TechnologiesNotFoundException_When_SomeTechnologiesDoNotExist() {
        // Arrange
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, VALID_TECHNOLOGY_IDS);

        when(capabilityRepository.findByName(VALID_NAME)).thenReturn(Mono.empty());
        when(technologyGateway.checkTechnologiesExistence(VALID_TECHNOLOGY_IDS)).thenReturn(Mono.just(MISSING_TECHNOLOGY_IDS));

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectError(TechnologiesNotFoundException.class)
                .verify();

        verify(capabilityRepository, never()).save(any());
    }

    @Test
    void Expect_CapabilityAlreadyExistsException_When_ExistingCapabilityIsCreated() {
        // Arrange
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, VALID_TECHNOLOGY_IDS);
        Capability createdCapability = capabilityWithStatus(CAPABILITY_ID, CapabilityStatusEnum.CREATED);

        when(capabilityRepository.findByName(VALID_NAME)).thenReturn(Mono.just(createdCapability));

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectError(CapabilityAlreadyExistsException.class)
                .verify();

        verify(technologyGateway, never()).checkTechnologiesExistence(anyList());
        verify(capabilityRepository, never()).save(any());
    }

    @Test
    void Expect_CapabilityToBeResumedAndCreated_When_ExistingCapabilityIsCreating() {
        // Arrange: un intento anterior murió a mitad de camino y dejó la capability en CREATING
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, VALID_TECHNOLOGY_IDS);
        Capability creatingCapability = capabilityWithStatus(CAPABILITY_ID, CapabilityStatusEnum.CREATING);
        Capability createdCapability = capabilityWithStatus(CAPABILITY_ID, CapabilityStatusEnum.CREATED);

        when(capabilityRepository.findByName(VALID_NAME)).thenReturn(Mono.just(creatingCapability));
        when(technologyGateway.checkTechnologiesExistence(VALID_TECHNOLOGY_IDS)).thenReturn(Mono.just(List.of()));
        when(capabilityRepository.save(argThat(c -> c != null && CAPABILITY_ID.equals(c.getId()))))
                .thenReturn(Mono.just(creatingCapability), Mono.just(createdCapability));
        when(technologyGateway.deleteCapabilityTechnologies(CAPABILITY_ID)).thenReturn(Mono.empty());
        when(technologyGateway.linkCapabilityTechnologies(CAPABILITY_ID, VALID_TECHNOLOGY_IDS)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectNextMatches(result -> result.getId().equals(CAPABILITY_ID)
                        && result.getStatus() == CapabilityStatusEnum.CREATED)
                .verifyComplete();

        // el mismo id se reutiliza y se limpian los vínculos parciales antes de volver a enlazar
        verify(technologyGateway).deleteCapabilityTechnologies(CAPABILITY_ID);
        verify(technologyGateway).linkCapabilityTechnologies(CAPABILITY_ID, VALID_TECHNOLOGY_IDS);
    }

    @Test
    void Expect_CapabilityToRemainCreating_When_LinkingTechnologiesFails() {
        // Arrange
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, VALID_TECHNOLOGY_IDS);
        Capability creatingCapability = capabilityWithStatus(CAPABILITY_ID, CapabilityStatusEnum.CREATING);
        RuntimeException linkFailure = new RuntimeException("technology service unavailable");

        when(capabilityRepository.findByName(VALID_NAME)).thenReturn(Mono.empty());
        when(technologyGateway.checkTechnologiesExistence(VALID_TECHNOLOGY_IDS)).thenReturn(Mono.just(List.of()));
        when(capabilityRepository.save(any(Capability.class))).thenReturn(Mono.just(creatingCapability));
        when(technologyGateway.linkCapabilityTechnologies(CAPABILITY_ID, VALID_TECHNOLOGY_IDS)).thenReturn(Mono.error(linkFailure));

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectErrorMatches(error -> error == linkFailure)
                .verify();

        // no se marca CREATED: la capability queda en CREATING para que un reintento la repare
        verify(capabilityRepository, times(1)).save(any(Capability.class));
    }

    @Test
    void Expect_FieldsValidationException_When_NameIsBlank() {
        // Arrange
        CapabilityCreateCommand command = new CapabilityCreateCommand(BLANK_VALUE, VALID_DESCRIPTION, VALID_TECHNOLOGY_IDS);

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(technologyGateway, never()).checkTechnologiesExistence(anyList());
    }

    @Test
    void Expect_FieldsValidationException_When_DescriptionIsBlank() {
        // Arrange
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, BLANK_VALUE, VALID_TECHNOLOGY_IDS);

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(technologyGateway, never()).checkTechnologiesExistence(anyList());
    }

    @Test
    void Expect_FieldsValidationException_When_TechnologyIdsHasFewerThanThreeElements() {
        // Arrange
        List<Long> tooFewIds = List.of(TECHNOLOGY_ID_1, TECHNOLOGY_ID_2);
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, tooFewIds);

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(technologyGateway, never()).checkTechnologiesExistence(anyList());
    }

    @Test
    void Expect_FieldsValidationException_When_TechnologyIdsHasDuplicates() {
        // Arrange
        List<Long> duplicatedIds = List.of(TECHNOLOGY_ID_1, TECHNOLOGY_ID_1, TECHNOLOGY_ID_2);
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, duplicatedIds);

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(technologyGateway, never()).checkTechnologiesExistence(anyList());
    }

    @Test
    void Expect_FieldsValidationException_When_TechnologyIdsHasMoreThanTwentyElements() {
        // Arrange
        List<Long> tooManyIds = LongStream.rangeClosed(1, CapabilityTechnologyIds.MAX_SIZE + 1).boxed().toList();
        CapabilityCreateCommand command = new CapabilityCreateCommand(VALID_NAME, VALID_DESCRIPTION, tooManyIds);

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(technologyGateway, never()).checkTechnologiesExistence(anyList());
    }

    private static Capability capabilityWithStatus(Long id, CapabilityStatusEnum status) {
        return Capability.builder()
                .id(id)
                .name(VALID_NAME)
                .description(VALID_DESCRIPTION)
                .status(status)
                .build();
    }
}
