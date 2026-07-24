package co.com.pragma.r2dbc;

import co.com.pragma.model.capabilitybootcamp.CapabilityBootcamp;
import co.com.pragma.r2dbc.entity.CapabilityBootcampEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityBootcampReactiveRepositoryAdapterTest {

    private static final Long BOOTCAMP_ID = 10L;
    private static final Long CAPABILITY_ID = 1L;
    private static final Long RELATION_ID = 100L;

    @Mock
    private CapabilityBootcampReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    private CapabilityBootcampReactiveRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CapabilityBootcampReactiveRepositoryAdapter(repository, mapper);
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
}
