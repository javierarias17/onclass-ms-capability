package co.com.pragma.consumer;

import co.com.pragma.consumer.dto.CapabilityTechnologyLinkInDto;
import co.com.pragma.consumer.dto.TechnologiesByCapabilityEntryDto;
import co.com.pragma.consumer.dto.TechnologiesByCapabilityInDto;
import co.com.pragma.consumer.dto.TechnologiesByCapabilityOutDto;
import co.com.pragma.consumer.dto.TechnologyExistenceInDto;
import co.com.pragma.consumer.dto.TechnologyExistenceOutDto;
import co.com.pragma.model.capability.exceptions.TechnologyServiceUnavailableException;
import co.com.pragma.model.capability.gateways.TechnologyGateway;
import co.com.pragma.model.capability.query.TechnologySummary;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechnologyRestConsumer implements TechnologyGateway {

    private static final String EXISTENCE_CHECK_PATH = "/api/v1/technologies/existence-check";
    private static final String CAPABILITY_TECHNOLOGIES_PATH = "/api/v1/capability-technologies";
    private static final String CAPABILITY_TECHNOLOGIES_BY_CAPABILITY_IDS_PATH = "/api/v1/capability-technologies/by-capability-ids";
    private static final String DELETE_CAPABILITY_TECHNOLOGIES_PATH = CAPABILITY_TECHNOLOGIES_PATH + "/{capabilityId}";

    private static final String SERVICE_CALL_FAILED_MESSAGE = "Unable to reach technology service at %s";
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
                .retryWhen(transientErrorRetry())
                .onErrorMap(error -> new TechnologyServiceUnavailableException(
                        buildServiceCallFailedMessage(EXISTENCE_CHECK_PATH), error));
    }

    @Override
    @CircuitBreaker(name = "linkCapabilityTechnologies")
    public Mono<Void> linkCapabilityTechnologies(Long capabilityId, List<Long> technologyIds) {
        return client.post()
                .uri(CAPABILITY_TECHNOLOGIES_PATH)
                .bodyValue(new CapabilityTechnologyLinkInDto(capabilityId, technologyIds))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(transientErrorRetry())
                .onErrorMap(error -> new TechnologyServiceUnavailableException(
                        buildServiceCallFailedMessage(CAPABILITY_TECHNOLOGIES_PATH), error));
    }

    @Override
    @CircuitBreaker(name = "deleteCapabilityTechnologies")
    public Mono<Void> deleteCapabilityTechnologies(Long capabilityId) {
        return client.delete()
                .uri(DELETE_CAPABILITY_TECHNOLOGIES_PATH, capabilityId)
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(transientErrorRetry())
                .onErrorMap(error -> new TechnologyServiceUnavailableException(
                        buildServiceCallFailedMessage(DELETE_CAPABILITY_TECHNOLOGIES_PATH), error));
    }

    @Override
    @CircuitBreaker(name = "findTechnologiesByCapabilityIds")
    public Mono<Map<Long, List<TechnologySummary>>> findTechnologiesByCapabilityIds(List<Long> capabilityIds) {
        return client.post()
                .uri(CAPABILITY_TECHNOLOGIES_BY_CAPABILITY_IDS_PATH)
                .bodyValue(new TechnologiesByCapabilityInDto(capabilityIds))
                .retrieve()
                .bodyToMono(TechnologiesByCapabilityOutDto.class)
                .map(this::toTechnologiesByCapabilityMap)
                .retryWhen(transientErrorRetry())
                .onErrorMap(error -> new TechnologyServiceUnavailableException(
                        buildServiceCallFailedMessage(CAPABILITY_TECHNOLOGIES_BY_CAPABILITY_IDS_PATH), error));
    }

    private static String buildServiceCallFailedMessage(String path) {
        return SERVICE_CALL_FAILED_MESSAGE.formatted(path);
    }

    private Map<Long, List<TechnologySummary>> toTechnologiesByCapabilityMap(
            TechnologiesByCapabilityOutDto response) {
        return response.capabilities().stream()
                .collect(Collectors.toMap(
                        TechnologiesByCapabilityEntryDto::capabilityId,
                        entry -> entry.technologies().stream()
                                .map(technology -> new TechnologySummary(technology.id(), technology.name()))
                                .toList()));
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
