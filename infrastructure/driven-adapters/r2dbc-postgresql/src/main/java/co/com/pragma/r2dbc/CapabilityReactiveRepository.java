package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.CapabilityEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CapabilityReactiveRepository extends
        ReactiveCrudRepository<CapabilityEntity, Long>,
        ReactiveQueryByExampleExecutor<CapabilityEntity> {

    Mono<CapabilityEntity> findByNameIgnoreCase(String name);
}
