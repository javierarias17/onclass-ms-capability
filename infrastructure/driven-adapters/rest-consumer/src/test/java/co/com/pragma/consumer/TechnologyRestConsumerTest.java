package co.com.pragma.consumer;

import co.com.pragma.model.capability.query.TechnologySummary;
import co.com.pragma.model.capability.exceptions.TechnologyServiceUnavailableException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

class TechnologyRestConsumerTest {

    private static final Long CAPABILITY_ID = 10L;
    private static final Long TECHNOLOGY_ID_1 = 1L;
    private static final Long TECHNOLOGY_ID_2 = 2L;
    private static final Long TECHNOLOGY_ID_3 = 3L;
    private static final List<Long> TECHNOLOGY_IDS = List.of(TECHNOLOGY_ID_1, TECHNOLOGY_ID_2, TECHNOLOGY_ID_3);
    private static final Long GROUPED_CAPABILITY_ID = 1L;
    private static final Long GROUPED_TECHNOLOGY_ID = 10L;
    private static final String GROUPED_TECHNOLOGY_NAME = "Java";

    private static TechnologyRestConsumer technologyRestConsumer;

    private static MockWebServer mockBackEnd;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();
        technologyRestConsumer = new TechnologyRestConsumer(webClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void When_AllTechnologiesExist_Expect_EmptyMissingIdsList() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"missingIds\": []}"));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.checkTechnologiesExistence(TECHNOLOGY_IDS))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void When_SomeTechnologiesDoNotExist_Expect_MissingIdsListToBeReturned() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"missingIds\": [%d]}".formatted(TECHNOLOGY_ID_3)));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.checkTechnologiesExistence(TECHNOLOGY_IDS))
                .expectNextMatches(missingIds -> missingIds.equals(List.of(TECHNOLOGY_ID_3)))
                .verifyComplete();
    }

    @Test
    void When_LinkingTechnologies_Expect_CompletionWithoutError() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.CREATED.value())
                .setBody("{\"capabilityId\": %d, \"technologyIds\": [%d, %d, %d]}"
                        .formatted(CAPABILITY_ID, TECHNOLOGY_ID_1, TECHNOLOGY_ID_2, TECHNOLOGY_ID_3)));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.linkCapabilityTechnologies(CAPABILITY_ID, TECHNOLOGY_IDS))
                .verifyComplete();
    }

    @Test
    void When_DeletingCapabilityTechnologies_Expect_CompletionWithoutError() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value()));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.deleteCapabilityTechnologies(CAPABILITY_ID))
                .verifyComplete();
    }

    @Test
    void When_FindingTechnologiesByCapabilityIds_Expect_TechnologiesGroupedByCapability() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("""
                        {
                          "capabilities": [
                            { "capabilityId": %d, "technologies": [ { "id": %d, "name": "%s" } ] }
                          ]
                        }
                        """.formatted(GROUPED_CAPABILITY_ID, GROUPED_TECHNOLOGY_ID, GROUPED_TECHNOLOGY_NAME)));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.findTechnologiesByCapabilityIds(List.of(GROUPED_CAPABILITY_ID)))
                .expectNextMatches(result -> result.equals(Map.of(GROUPED_CAPABILITY_ID,
                        List.of(new TechnologySummary(GROUPED_TECHNOLOGY_ID, GROUPED_TECHNOLOGY_NAME)))))
                .verifyComplete();
    }

    @Test
    void When_FindingTechnologiesByCapabilityIdsFails_Expect_TechnologyServiceUnavailableException() {
        // Arrange: solo se encola UNA respuesta 400 (no transitoria, no hay reintento);
        // el consumer debe traducir cualquier falla de este endpoint a un 503 para el cliente.
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .setBody("{\"message\": \"Business validation failed\"}"));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.findTechnologiesByCapabilityIds(List.of(GROUPED_CAPABILITY_ID)))
                .expectError(TechnologyServiceUnavailableException.class)
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void When_ServerRespondsWithTransientServerError_Expect_RetryUntilSuccess() {
        // Arrange: primer intento falla con 500, segundo intento (reintento) responde bien
        mockBackEnd.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"missingIds\": []}"));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.checkTechnologiesExistence(TECHNOLOGY_IDS))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void When_ServerRespondsWithBusinessError_Expect_NoRetryAndTechnologyServiceUnavailableException() {
        // Arrange: solo se encola UNA respuesta 400; si el consumer reintentara,
        // la segunda llamada se quedaría esperando una respuesta que no existe y el test fallaría por timeout.
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .setBody("{\"message\": \"Business validation failed\"}"));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.checkTechnologiesExistence(TECHNOLOGY_IDS))
                .expectError(TechnologyServiceUnavailableException.class)
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void When_LinkingTechnologiesFails_Expect_TechnologyServiceUnavailableException() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .setBody("{\"message\": \"Business validation failed\"}"));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.linkCapabilityTechnologies(CAPABILITY_ID, TECHNOLOGY_IDS))
                .expectError(TechnologyServiceUnavailableException.class)
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void When_DeletingCapabilityTechnologiesFails_Expect_TechnologyServiceUnavailableException() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .setBody("{\"message\": \"Business validation failed\"}"));

        // Act & Assert
        StepVerifier.create(technologyRestConsumer.deleteCapabilityTechnologies(CAPABILITY_ID))
                .expectError(TechnologyServiceUnavailableException.class)
                .verify(Duration.ofSeconds(2));
    }
}
