package co.com.pragma.usecase.checkcapabilitiesexistence;

import co.com.pragma.model.capability.gateways.CapabilityRepository;
import co.com.pragma.model.exceptions.FieldsValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckCapabilitiesExistenceUseCaseTest {

    private static final Long CAPABILITY_ID_1 = 1L;
    private static final Long CAPABILITY_ID_2 = 2L;
    private static final Long CAPABILITY_ID_3 = 3L;
    private static final Long MISSING_CAPABILITY_ID = 99L;

    @Mock
    private CapabilityRepository capabilityRepository;

    private CheckCapabilitiesExistenceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CheckCapabilitiesExistenceUseCase(capabilityRepository);
    }

    @Test
    void When_AllCapabilityIdsExist_Expect_EmptyMissingIdsList() {
        // Arrange
        List<Long> capabilityIds = List.of(CAPABILITY_ID_1, CAPABILITY_ID_2, CAPABILITY_ID_3);

        when(capabilityRepository.findMissingIds(capabilityIds)).thenReturn(Mono.just(List.of()));

        // Act & Assert
        StepVerifier.create(useCase.execute(capabilityIds))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void When_SomeCapabilityIdsDoNotExist_Expect_MissingIdsListToBeReturned() {
        // Arrange
        List<Long> capabilityIds = List.of(CAPABILITY_ID_1, CAPABILITY_ID_2, MISSING_CAPABILITY_ID);
        List<Long> missingIds = List.of(MISSING_CAPABILITY_ID);

        when(capabilityRepository.findMissingIds(capabilityIds)).thenReturn(Mono.just(missingIds));

        // Act & Assert
        StepVerifier.create(useCase.execute(capabilityIds))
                .expectNextMatches(result -> result.equals(missingIds))
                .verifyComplete();
    }

    @Test
    void Expect_FieldsValidationException_When_CapabilityIdsListIsEmpty() {
        // Act & Assert
        StepVerifier.create(useCase.execute(List.of()))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findMissingIds(anyList());
    }

    @Test
    void Expect_FieldsValidationException_When_CapabilityIdsListIsNull() {
        // Act & Assert
        StepVerifier.create(useCase.execute(null))
                .expectError(FieldsValidationException.class)
                .verify();

        verify(capabilityRepository, never()).findMissingIds(anyList());
    }
}
