package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.CapabilityBootcampEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CapabilityBootcampReactiveRepository extends
        ReactiveCrudRepository<CapabilityBootcampEntity, Long>,
        ReactiveQueryByExampleExecutor<CapabilityBootcampEntity> {

    @Query("INSERT INTO capability_bootcamps (bootcamp_id, capability_id) "
            + "VALUES (:bootcampId, :capabilityId) "
            + "ON CONFLICT (bootcamp_id, capability_id) DO NOTHING "
            + "RETURNING *")
    Mono<CapabilityBootcampEntity> insertIgnoringConflict(@Param("bootcampId") Long bootcampId,
            @Param("capabilityId") Long capabilityId);

    @Query("SELECT cb.capability_id FROM capability_bootcamps cb "
            + "JOIN capabilities c ON c.id = cb.capability_id "
            + "WHERE cb.bootcamp_id = :bootcampId AND cb.capability_id IN (:capabilityIds) AND c.status = 'DELETING'")
    Flux<Long> findDeletingCapabilityIds(@Param("bootcampId") Long bootcampId,
            @Param("capabilityIds") List<Long> capabilityIds);

    Mono<Void> deleteByBootcampId(Long bootcampId);

    Flux<CapabilityBootcampEntity> findByBootcampIdIn(List<Long> bootcampIds);

    @Query("SELECT DISTINCT capability_id FROM capability_bootcamps WHERE bootcamp_id = :bootcampId")
    Flux<Long> findCapabilityIdsByBootcampId(@Param("bootcampId") Long bootcampId);

    @Query("SELECT DISTINCT capability_id FROM capability_bootcamps "
            + "WHERE capability_id IN (:capabilityIds) AND bootcamp_id <> :bootcampId")
    Flux<Long> findCapabilityIdsReferencedByOtherBootcamps(@Param("capabilityIds") List<Long> capabilityIds,
            @Param("bootcampId") Long bootcampId);
}
