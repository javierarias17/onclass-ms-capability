package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.CapabilityEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CapabilityReactiveRepository extends
        ReactiveCrudRepository<CapabilityEntity, Long>,
        ReactiveQueryByExampleExecutor<CapabilityEntity> {

    String CREATED_STATUS = "CREATED";
    String DELETING_STATUS = "DELETING";

    Mono<CapabilityEntity> findByNameIgnoreCase(String name);

    @Query("UPDATE capabilities SET status = '" + DELETING_STATUS + "' WHERE id IN (:capabilityIds)")
    Mono<Void> markAsDeleting(@Param("capabilityIds") List<Long> capabilityIds);

    @Query("SELECT id FROM capabilities WHERE id IN (:capabilityIds) AND status = '" + CREATED_STATUS + "'")
    Flux<Long> findCreatedIds(@Param("capabilityIds") List<Long> capabilityIds);

    @Query("SELECT * FROM capabilities WHERE status = '" + CREATED_STATUS
            + "' ORDER BY LOWER(name) ASC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByNameAsc(@Param("size") int size, @Param("offset") long offset);

    @Query("SELECT * FROM capabilities WHERE status = '" + CREATED_STATUS
            + "' ORDER BY LOWER(name) DESC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByNameDesc(@Param("size") int size, @Param("offset") long offset);

    @Query("SELECT * FROM capabilities WHERE status = '" + CREATED_STATUS
            + "' ORDER BY technology_count ASC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByTechnologyCountAsc(@Param("size") int size, @Param("offset") long offset);

    @Query("SELECT * FROM capabilities WHERE status = '" + CREATED_STATUS
            + "' ORDER BY technology_count DESC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByTechnologyCountDesc(@Param("size") int size, @Param("offset") long offset);

    @Query("SELECT * FROM capabilities WHERE id IN (:capabilityIds) AND status = '" + CREATED_STATUS + "'")
    Flux<CapabilityEntity> findByIdIn(@Param("capabilityIds") List<Long> capabilityIds);

    @Query("DELETE FROM capabilities WHERE id IN (:candidateIds) "
            + "AND id NOT IN (SELECT capability_id FROM capability_bootcamps WHERE capability_id IN (:candidateIds)) "
            + "RETURNING id")
    Flux<Long> deleteOrphaned(@Param("candidateIds") List<Long> candidateIds);
}
