package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.CapabilityBootcampEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CapabilityBootcampReactiveRepository extends
        ReactiveCrudRepository<CapabilityBootcampEntity, Long>,
        ReactiveQueryByExampleExecutor<CapabilityBootcampEntity> {

    @Query("INSERT INTO capability_bootcamps (bootcamp_id, capability_id) "
            + "VALUES (:bootcampId, :capabilityId) "
            + "ON CONFLICT (bootcamp_id, capability_id) DO NOTHING "
            + "RETURNING *")
    Mono<CapabilityBootcampEntity> insertIgnoringConflict(@Param("bootcampId") Long bootcampId,
            @Param("capabilityId") Long capabilityId);
}
