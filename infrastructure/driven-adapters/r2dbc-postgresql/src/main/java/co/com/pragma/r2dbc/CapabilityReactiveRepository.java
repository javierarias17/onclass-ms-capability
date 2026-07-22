package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.CapabilityEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CapabilityReactiveRepository extends
        ReactiveCrudRepository<CapabilityEntity, Long>,
        ReactiveQueryByExampleExecutor<CapabilityEntity> {

    String COMPLETE_STATUS = "COMPLETE";

    Mono<CapabilityEntity> findByNameIgnoreCase(String name);

    @Query("SELECT * FROM capabilities WHERE status = '" + COMPLETE_STATUS
            + "' ORDER BY LOWER(name) ASC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByNameAsc(@Param("size") int size, @Param("offset") long offset);

    @Query("SELECT * FROM capabilities WHERE status = '" + COMPLETE_STATUS
            + "' ORDER BY LOWER(name) DESC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByNameDesc(@Param("size") int size, @Param("offset") long offset);

    @Query("SELECT * FROM capabilities WHERE status = '" + COMPLETE_STATUS
            + "' ORDER BY technology_count ASC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByTechnologyCountAsc(@Param("size") int size, @Param("offset") long offset);

    @Query("SELECT * FROM capabilities WHERE status = '" + COMPLETE_STATUS
            + "' ORDER BY technology_count DESC LIMIT :size OFFSET :offset")
    Flux<CapabilityEntity> findPageByTechnologyCountDesc(@Param("size") int size, @Param("offset") long offset);
}
