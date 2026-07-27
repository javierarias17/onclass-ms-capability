package co.com.pragma.r2dbc;

import co.com.pragma.model.capability.Capability;
import co.com.pragma.model.capability.query.CapabilitySortFieldEnum;
import co.com.pragma.model.capability.CapabilityStatusEnum;
import co.com.pragma.model.capability.query.SortDirectionEnum;
import co.com.pragma.model.capability.exceptions.CapabilityAlreadyExistsException;
import co.com.pragma.r2dbc.entity.CapabilityEntity;
import co.com.pragma.r2dbc.mapper.CapabilityEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityReactiveRepositoryAdapterTest {

    private static final String VALID_NAME = "Backend";
    private static final String VALID_DESCRIPTION = "Backend development capability";
    private static final Long CAPABILITY_ID = 1L;
    private static final int TECHNOLOGY_COUNT = 3;
    private static final int NO_TECHNOLOGIES_YET = 0;
    private static final int PAGE = 2;
    private static final int SIZE = 10;
    private static final long OFFSET = 20L;
    private static final int SMALL_SIZE = 5;
    private static final long ZERO_OFFSET = 0L;
    private static final long TOTAL_ELEMENTS = 5L;
    private static final String STATUS_COLUMN = "status";

    @Mock
    private CapabilityReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private CapabilityEntityMapper capabilityEntityMapper;

    @Mock
    private R2dbcEntityTemplate template;

    private CapabilityReactiveRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CapabilityReactiveRepositoryAdapter(repository, mapper, capabilityEntityMapper, template);
    }

    @Test
    void Expect_CapabilityAlreadyExistsException_When_ConcurrentInsertViolatesUniqueIndex() {
        // Arrange: dos requests concurrentes con el mismo nombre pasan el chequeo previo
        // y ambas intentan el INSERT; la segunda choca contra el índice único en Postgres.
        Capability capability = Capability.builder()
                .name(VALID_NAME)
                .description(VALID_DESCRIPTION)
                .status(CapabilityStatusEnum.CREATING)
                .build();
        CapabilityEntity entity = new CapabilityEntity(null, VALID_NAME, VALID_DESCRIPTION,
                CapabilityStatusEnum.CREATING.name(), NO_TECHNOLOGIES_YET, null);

        when(capabilityEntityMapper.toEntity(capability)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.error(
                new DuplicateKeyException("duplicate key value violates unique constraint")));

        // Act & Assert
        StepVerifier.create(adapter.save(capability))
                .expectError(CapabilityAlreadyExistsException.class)
                .verify();
    }

    @Test
    void Expect_FindPageByNameAsc_When_SortingByNameAscending() {
        // Arrange
        CapabilityEntity entity = new CapabilityEntity(CAPABILITY_ID, VALID_NAME, VALID_DESCRIPTION,
                CapabilityStatusEnum.CREATED.name(), TECHNOLOGY_COUNT, null);
        Capability capability = Capability.builder()
                .id(CAPABILITY_ID).name(VALID_NAME).description(VALID_DESCRIPTION)
                .status(CapabilityStatusEnum.CREATED).technologyCount(TECHNOLOGY_COUNT).build();

        when(repository.findPageByNameAsc(SIZE, OFFSET)).thenReturn(Flux.just(entity));
        when(capabilityEntityMapper.toDomain(entity)).thenReturn(capability);

        // Act & Assert: page 2, size 10 -> offset 20
        StepVerifier.create(adapter.findPage(PAGE, SIZE, CapabilitySortFieldEnum.NAME, SortDirectionEnum.ASC))
                .expectNextMatches(page -> page.size() == 1 && page.get(0).getId().equals(CAPABILITY_ID))
                .verifyComplete();

        verify(repository, never()).findPageByNameDesc(anyInt(), anyLong());
        verify(repository, never()).findPageByTechnologyCountAsc(anyInt(), anyLong());
        verify(repository, never()).findPageByTechnologyCountDesc(anyInt(), anyLong());
    }

    @Test
    void Expect_FindPageByNameDesc_When_SortingByNameDescending() {
        // Arrange
        when(repository.findPageByNameDesc(SMALL_SIZE, ZERO_OFFSET)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(adapter.findPage(0, SMALL_SIZE, CapabilitySortFieldEnum.NAME, SortDirectionEnum.DESC))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void Expect_FindPageByTechnologyCountAsc_When_SortingByTechnologyCountAscending() {
        // Arrange
        when(repository.findPageByTechnologyCountAsc(SMALL_SIZE, ZERO_OFFSET)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(adapter.findPage(0, SMALL_SIZE, CapabilitySortFieldEnum.TECHNOLOGY_COUNT, SortDirectionEnum.ASC))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void Expect_FindPageByTechnologyCountDesc_When_SortingByTechnologyCountDescending() {
        // Arrange
        when(repository.findPageByTechnologyCountDesc(SMALL_SIZE, ZERO_OFFSET)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(adapter.findPage(0, SMALL_SIZE, CapabilitySortFieldEnum.TECHNOLOGY_COUNT, SortDirectionEnum.DESC))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void Expect_OnlyCompleteCapabilities_When_Counting() {
        // Arrange
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(template.count(queryCaptor.capture(), eqCapabilityEntityClass()))
                .thenReturn(Mono.just(TOTAL_ELEMENTS));

        // Act & Assert
        StepVerifier.create(adapter.count())
                .expectNext(TOTAL_ELEMENTS)
                .verifyComplete();

        // el conteo debe filtrar por status = CREATED, igual que los findPageBy*
        Criteria criteria = (Criteria) queryCaptor.getValue().getCriteria().orElseThrow();
        assertEquals(STATUS_COLUMN, criteria.getColumn().getReference());
        assertEquals(Criteria.Comparator.EQ, criteria.getComparator());
        assertEquals(CapabilityReactiveRepository.CREATED_STATUS, criteria.getValue());
    }

    @Test
    void When_AllCapabilityIdsExistAndComplete_Expect_EmptyMissingIdsList() {
        // Arrange
        List<Long> capabilityIds = List.of(1L, 2L);

        when(repository.findCreatedIds(capabilityIds)).thenReturn(Flux.just(1L, 2L));

        // Act & Assert
        StepVerifier.create(adapter.findMissingIds(capabilityIds))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void When_SomeCapabilityIdsDoNotExist_Expect_MissingIdsListToBeReturned() {
        // Arrange
        List<Long> capabilityIds = List.of(1L, 2L, 99L);

        when(repository.findCreatedIds(capabilityIds)).thenReturn(Flux.just(1L, 2L));

        // Act & Assert
        StepVerifier.create(adapter.findMissingIds(capabilityIds))
                .expectNextMatches(missingIds -> missingIds.equals(List.of(99L)))
                .verifyComplete();
    }

    @Test
    void When_CapabilityIsStillPending_Expect_ItToBeTreatedAsMissing() {
        // Arrange: la capacidad existe en la tabla pero su saga de registro no terminó
        // (nunca se confirmó el vínculo con tecnologías), así que no debe ser referenciable.
        // findCreatedIds ya filtra por status = CREATED en la query, por lo que el id 2
        // (CREATING) simplemente no aparece en el resultado.
        List<Long> capabilityIds = List.of(1L, 2L);

        when(repository.findCreatedIds(capabilityIds)).thenReturn(Flux.just(1L));

        // Act & Assert
        StepVerifier.create(adapter.findMissingIds(capabilityIds))
                .expectNextMatches(missingIds -> missingIds.equals(List.of(2L)))
                .verifyComplete();
    }

    @Test
    void When_FindingCapabilitiesByIds_Expect_MatchingDomainCapabilitiesReturned() {
        // Arrange
        List<Long> capabilityIds = List.of(CAPABILITY_ID);
        CapabilityEntity entity = new CapabilityEntity(CAPABILITY_ID, VALID_NAME, VALID_DESCRIPTION,
                CapabilityStatusEnum.CREATED.name(), TECHNOLOGY_COUNT, null);
        Capability capability = Capability.builder()
                .id(CAPABILITY_ID).name(VALID_NAME).description(VALID_DESCRIPTION)
                .status(CapabilityStatusEnum.CREATED).technologyCount(TECHNOLOGY_COUNT).build();

        when(repository.findByIdIn(capabilityIds)).thenReturn(Flux.just(entity));
        when(capabilityEntityMapper.toDomain(entity)).thenReturn(capability);

        // Act & Assert
        StepVerifier.create(adapter.findByIds(capabilityIds))
                .expectNextMatches(capabilities -> capabilities.size() == 1
                        && capabilities.get(0).getId().equals(CAPABILITY_ID))
                .verifyComplete();
    }

    @Test
    void When_MarkingCapabilitiesAsDeleting_Expect_RepositoryToBeCalled() {
        // Arrange
        List<Long> capabilityIds = List.of(CAPABILITY_ID);
        when(repository.markAsDeleting(capabilityIds)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(adapter.markAsDeleting(capabilityIds))
                .verifyComplete();

        verify(repository).markAsDeleting(capabilityIds);
    }

    private static Class<CapabilityEntity> eqCapabilityEntityClass() {
        return org.mockito.ArgumentMatchers.eq(CapabilityEntity.class);
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
