package co.com.pragma.usecase.deletebootcampcapabilities;

import co.com.pragma.model.capabilitybootcamp.gateways.CapabilityBootcampRepository;
import co.com.pragma.model.exceptions.FieldsValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteBootcampCapabilitiesUseCaseTest {

    private static final String BOOTCAMP_ID = "10";
    private static final Long BOOTCAMP_ID_VALUE = 10L;
    private static final String BLANK_BOOTCAMP_ID = "  ";
    private static final String NOT_NUMERIC_BOOTCAMP_ID = "1022-";

    @Mock
    private CapabilityBootcampRepository capabilityBootcampRepository;

    private DeleteBootcampCapabilitiesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteBootcampCapabilitiesUseCase(capabilityBootcampRepository);
    }

    @Test
    void When_BootcampIdIsValid_Expect_LinksToBeDeleted() {
        // Arrange
        when(capabilityBootcampRepository.deleteByBootcampId(BOOTCAMP_ID_VALUE)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(useCase.execute(BOOTCAMP_ID))
                .verifyComplete();

        verify(capabilityBootcampRepository).deleteByBootcampId(BOOTCAMP_ID_VALUE);
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdIsNull() {
        // Act & Assert
        StepVerifier.create(useCase.execute(null))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).deleteByBootcampId(anyLong());
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdIsBlank() {
        // Act & Assert
        StepVerifier.create(useCase.execute(BLANK_BOOTCAMP_ID))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).deleteByBootcampId(anyLong());
    }

    @Test
    void Expect_FieldsValidationException_When_BootcampIdIsNotNumeric() {
        // Act & Assert
        StepVerifier.create(useCase.execute(NOT_NUMERIC_BOOTCAMP_ID))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityBootcampRepository, never()).deleteByBootcampId(anyLong());
    }
}
