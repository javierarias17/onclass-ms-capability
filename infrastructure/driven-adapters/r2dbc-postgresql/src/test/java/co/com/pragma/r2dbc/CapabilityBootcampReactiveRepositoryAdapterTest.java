package co.com.pragma.r2dbc;

import co.com.pragma.model.capability.exceptions.CapabilitiesNotFoundException;
import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.model.capabilitybootcamp.LinkBootcampCapabilities;
import co.com.pragma.model.common.FieldConstants;
import co.com.pragma.r2dbc.entity.CapabilityBootcampEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityBootcampReactiveRepositoryAdapterTest {

    private static final Long BOOTCAMP_ID = 10L;
    private static final Long CAPABILITY_ID = 1L;
    private static final Long OTHER_CAPABILITY_ID = 2L;
    private static final Long RELATION_ID = 100L;

    @Mock
    private CapabilityBootcampReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private CapabilityReactiveRepository capabilityRepository;

    private CapabilityBootcampReactiveRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CapabilityBootcampReactiveRepositoryAdapter(repository, mapper, capabilityRepository);
    }

    @Test
    void When_CapabilityIsNew_Expect_LinkToBeInserted() {
        // Arrange
        LinkBootcampCapabilities request = LinkBootcampCapabilities.builder()
                .bootcampId(BOOTCAMP_ID).capabilityIds(List.of(CAPABILITY_ID)).build();
        CapabilityBootcampEntity entity = CapabilityBootcampEntity.builder()
                .id(RELATION_ID).bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build();
        CapabilityBootcamp relation = CapabilityBootcamp.builder()
                .id(RELATION_ID).bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build();

        when(repository.insertIgnoringConflict(BOOTCAMP_ID, CAPABILITY_ID)).thenReturn(Mono.just(entity));
        when(mapper.map(entity, CapabilityBootcamp.class)).thenReturn(relation);

        // Act & Assert
        StepVerifier.create(adapter.saveAll(request))
                .expectNextMatches(relations -> relations.size() == 1
                        && relations.get(0).getCapabilityId().equals(CAPABILITY_ID))
                .verifyComplete();
    }

    @Test
    void Expect_CapabilitiesNotFoundException_When_CapabilityWasPhysicallyDeletedBeforeInsert() {
        // Arrange: OTHER_CAPABILITY_ID paso el chequeo de existencia previo, pero ya fue
        // borrada físicamente (FK) para cuando este INSERT corre -> Postgres rechaza SOLO ese
        // INSERT con una violación de foreign key; el error debe reportar únicamente ese id,
        // no toda la lista de capabilityIds que llegó por parámetro (CAPABILITY_ID sí existe).
        LinkBootcampCapabilities request = LinkBootcampCapabilities.builder()
                .bootcampId(BOOTCAMP_ID).capabilityIds(List.of(CAPABILITY_ID, OTHER_CAPABILITY_ID)).build();
        CapabilityBootcampEntity entity = CapabilityBootcampEntity.builder()
                .id(RELATION_ID).bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build();
        CapabilityBootcamp relation = CapabilityBootcamp.builder()
                .id(RELATION_ID).bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build();

        // lenient: flatMap cancela las suscripciones restantes en cuanto una falla, así que
        // no siempre se invoca el insert de CAPABILITY_ID antes de que la cadena se corte
        lenient().when(repository.insertIgnoringConflict(BOOTCAMP_ID, CAPABILITY_ID)).thenReturn(Mono.just(entity));
        lenient().when(mapper.map(entity, CapabilityBootcamp.class)).thenReturn(relation);
        lenient().when(repository.insertIgnoringConflict(BOOTCAMP_ID, OTHER_CAPABILITY_ID)).thenReturn(Mono.error(
                new DataIntegrityViolationException("insert or update on table \"capability_bootcamps\" "
                        + "violates foreign key constraint")));

        // Act & Assert
        StepVerifier.create(adapter.saveAll(request))
                .expectErrorMatches(error -> error instanceof CapabilitiesNotFoundException notFound
                        && notFound.getErrors().get(FieldConstants.CAPABILITY_IDS).contains(OTHER_CAPABILITY_ID.toString())
                        && !notFound.getErrors().get(FieldConstants.CAPABILITY_IDS).contains(CAPABILITY_ID.toString()))
                .verify();
    }

    @Test
    void When_LinkAlreadyExisted_Expect_SyntheticRelationReturned() {
        // Arrange: ON CONFLICT DO NOTHING no devuelve fila porque el link ya existía.
        LinkBootcampCapabilities request = LinkBootcampCapabilities.builder()
                .bootcampId(BOOTCAMP_ID).capabilityIds(List.of(CAPABILITY_ID)).build();

        when(repository.insertIgnoringConflict(BOOTCAMP_ID, CAPABILITY_ID)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(adapter.saveAll(request))
                .expectNextMatches(relations -> relations.size() == 1
                        && relations.get(0).getBootcampId().equals(BOOTCAMP_ID)
                        && relations.get(0).getCapabilityId().equals(CAPABILITY_ID))
                .verifyComplete();
    }

    @Test
    void When_SomeCapabilityIsDeleting_Expect_ItToBeReturnedByFindDeletingCapabilityIds() {
        // Arrange
        List<Long> capabilityIds = List.of(CAPABILITY_ID, OTHER_CAPABILITY_ID);
        LinkBootcampCapabilities request = LinkBootcampCapabilities.builder()
                .bootcampId(BOOTCAMP_ID).capabilityIds(capabilityIds).build();

        when(repository.findDeletingCapabilityIds(BOOTCAMP_ID, capabilityIds))
                .thenReturn(Flux.just(OTHER_CAPABILITY_ID));

        // Act & Assert
        StepVerifier.create(adapter.findDeletingCapabilityIds(request))
                .expectNextMatches(deletingIds -> deletingIds.equals(List.of(OTHER_CAPABILITY_ID)))
                .verifyComplete();
    }

    @Test
    void When_BootcampIdsHaveRelations_Expect_MatchingDomainRelationsReturned() {
        // Arrange
        List<Long> bootcampIds = List.of(BOOTCAMP_ID);
        CapabilityBootcampEntity entity = CapabilityBootcampEntity.builder()
                .id(RELATION_ID).bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build();
        CapabilityBootcamp relation = CapabilityBootcamp.builder()
                .id(RELATION_ID).bootcampId(BOOTCAMP_ID).capabilityId(CAPABILITY_ID).build();

        when(repository.findByBootcampIdIn(bootcampIds)).thenReturn(Flux.just(entity));
        when(mapper.map(entity, CapabilityBootcamp.class)).thenReturn(relation);

        // Act & Assert
        StepVerifier.create(adapter.findByBootcampIds(bootcampIds))
                .expectNextMatches(relations -> relations.size() == 1
                        && relations.get(0).getBootcampId().equals(BOOTCAMP_ID)
                        && relations.get(0).getCapabilityId().equals(CAPABILITY_ID))
                .verifyComplete();
    }

    @Test
    void When_BootcampIdsHaveNoRelations_Expect_EmptyList() {
        // Arrange
        List<Long> bootcampIds = List.of(BOOTCAMP_ID);

        when(repository.findByBootcampIdIn(bootcampIds)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(adapter.findByBootcampIds(bootcampIds))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void When_BootcampHasLinkedCapabilities_Expect_CapabilityIdsReturned() {
        // Arrange
        when(repository.findCapabilityIdsByBootcampId(BOOTCAMP_ID))
                .thenReturn(Flux.just(CAPABILITY_ID, OTHER_CAPABILITY_ID));

        // Act & Assert
        StepVerifier.create(adapter.findCapabilityIdsByBootcampId(BOOTCAMP_ID))
                .expectNextMatches(ids -> ids.equals(List.of(CAPABILITY_ID, OTHER_CAPABILITY_ID)))
                .verifyComplete();
    }

    @Test
    void When_BootcampHasNoLinkedCapabilities_Expect_EmptyIdList() {
        // Arrange
        when(repository.findCapabilityIdsByBootcampId(BOOTCAMP_ID)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(adapter.findCapabilityIdsByBootcampId(BOOTCAMP_ID))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void When_CapabilityIsReferencedByAnotherBootcamp_Expect_ItToBeReturnedAsSurvivor() {
        // Arrange
        List<Long> capabilityIds = List.of(CAPABILITY_ID, OTHER_CAPABILITY_ID);

        when(repository.findCapabilityIdsReferencedByOtherBootcamps(capabilityIds, BOOTCAMP_ID))
                .thenReturn(Flux.just(CAPABILITY_ID));

        // Act & Assert
        StepVerifier.create(adapter.findCapabilityIdsReferencedByOtherBootcamps(capabilityIds, BOOTCAMP_ID))
                .expectNextMatches(survivors -> survivors.equals(List.of(CAPABILITY_ID)))
                .verifyComplete();
    }

    @Test
    void When_ThereAreOrphanCandidates_Expect_LinksAndCapabilitiesToBeDeletedInOrder() {
        // Arrange: primero se borran los links del bootcamp, recién después las
        // capacidades candidatas (para que la revalidación del DELETE ya no vea
        // los links de este bootcamp).
        List<Long> candidates = List.of(CAPABILITY_ID);
        when(repository.deleteByBootcampId(BOOTCAMP_ID)).thenReturn(Mono.empty());
        when(capabilityRepository.deleteOrphaned(candidates)).thenReturn(Flux.just(CAPABILITY_ID));

        // Act & Assert
        StepVerifier.create(adapter.deleteLinksAndOrphanedCapabilities(BOOTCAMP_ID, candidates))
                .verifyComplete();

        verify(repository).deleteByBootcampId(BOOTCAMP_ID);
        verify(capabilityRepository).deleteOrphaned(candidates);
    }

    @Test
    void When_ThereAreNoOrphanCandidates_Expect_OnlyLinksToBeDeleted() {
        // Arrange: el bootcamp tenía capacidades, pero todas siguen en uso por otros
        // bootcamps, así que no hay nada que borrar en la tabla de capacidades.
        when(repository.deleteByBootcampId(BOOTCAMP_ID)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(adapter.deleteLinksAndOrphanedCapabilities(BOOTCAMP_ID, List.of()))
                .verifyComplete();

        verify(repository).deleteByBootcampId(BOOTCAMP_ID);
        verify(capabilityRepository, never()).deleteOrphaned(any());
    }
}
