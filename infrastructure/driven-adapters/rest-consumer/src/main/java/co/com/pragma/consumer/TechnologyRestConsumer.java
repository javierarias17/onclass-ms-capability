package co.com.pragma.consumer;

import co.com.pragma.consumer.dto.CapabilityTechnologyLinkInDto;
import co.com.pragma.consumer.dto.TechnologyExistenceInDto;
import co.com.pragma.consumer.dto.TechnologyExistenceOutDto;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnologyRestConsumer implements TechnologyGateway {

    private static final String EXISTENCE_CHECK_PATH = "/api/v1/technologies/existence-check";
    private static final String CAPABILITY_TECHNOLOGIES_PATH = "/api/v1/capability-technologies";
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(200);

    private final WebClient client;

    @Override
    @CircuitBreaker(name = "checkTechnologiesExistence")
    public Mono<List<Long>> checkTechnologiesExistence(List<Long> technologyIds) {
        return client.post()
                .uri(EXISTENCE_CHECK_PATH)
                .bodyValue(new TechnologyExistenceInDto(technologyIds))
                .retrieve()
                .bodyToMono(TechnologyExistenceOutDto.class)
                .map(TechnologyExistenceOutDto::missingIds)
                .retryWhen(transientErrorRetry());
    }

    @Override
    @CircuitBreaker(name = "linkCapabilityTechnologies")
    public Mono<Void> linkCapabilityTechnologies(Long capabilityId, List<Long> technologyIds) {
        return client.post()
                .uri(CAPABILITY_TECHNOLOGIES_PATH)
                .bodyValue(new CapabilityTechnologyLinkInDto(capabilityId, technologyIds))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(transientErrorRetry());
    }

    @Override
    @CircuitBreaker(name = "deleteCapabilityTechnologies")
    public Mono<Void> deleteCapabilityTechnologies(Long capabilityId) {
        return client.delete()
                .uri(CAPABILITY_TECHNOLOGIES_PATH + "/{capabilityId}", capabilityId)
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(transientErrorRetry());
    }

    private Retry transientErrorRetry() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                .filter(TechnologyRestConsumer::isTransientError);
    }

    private static boolean isTransientError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException)
            return responseException.getStatusCode().is5xxServerError();
        return throwable instanceof WebClientRequestException;
    }
}
