package co.com.pragma.api.exceptions;

import co.com.pragma.model.capability.exceptions.CapabilityAlreadyExistsException;
import co.com.pragma.model.capability.exceptions.ServiceUnavailableException;
import co.com.pragma.model.exceptions.FunctionalException;
import co.com.pragma.model.exceptions.TechnicalException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final String MESSAGE = "message";
    private static final String ERRORS = "errors";
    private static final String FIELD = "field";
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. Please contact the administrator.";
    private static final String SERVICE_UNAVAILABLE_MESSAGE = "The service is temporarily unavailable. Please try again shortly.";
    private static final String TECHINICAL_ERROR_MESSAGE_LOG = "Technical error: {}";
    private static final String UNEXPECTED_ERROR_MESSAGE_LOG = "Unexpected error";
    private static final String CIRCUIT_BREAKER_OPEN_MESSAGE_LOG = "Circuit breaker open: {}";
    private static final String SERVICE_UNAVAILABLE_MESSAGE_LOG = "Downstream service unavailable: {}";

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Map<Class<?>, HttpStatus> HTTP_STATUS_CODES = new HashMap<>();

    static {
        HTTP_STATUS_CODES.put(CapabilityAlreadyExistsException.class, HttpStatus.CONFLICT);
    }

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
        this.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    private Mono<ServerResponse> buildErrorResponse(ServerRequest request) {
        Throwable throwable = getError(request);
        Map<String, Object> responseBody = new LinkedHashMap<>();
        HttpStatus status;

        if (throwable instanceof FunctionalException functionalException) {
            status = HTTP_STATUS_CODES.getOrDefault(throwable.getClass(), HttpStatus.BAD_REQUEST);
            responseBody.put(MESSAGE, functionalException.getMessage());
            if (functionalException.getErrors() != null && !functionalException.getErrors().isEmpty()) {
                responseBody.put(ERRORS, toErrorList(functionalException.getErrors()));
            }
        } else if (throwable instanceof CallNotPermittedException) {
            logger.error(CIRCUIT_BREAKER_OPEN_MESSAGE_LOG, throwable.getMessage());
            status = HttpStatus.SERVICE_UNAVAILABLE;
            responseBody.put(MESSAGE, SERVICE_UNAVAILABLE_MESSAGE);
        } else if (throwable instanceof ServiceUnavailableException) {
            // se loguea el throwable completo (no solo el mensaje) para conservar la causa técnica original;
            // el mensaje de la excepción ya identifica qué servicio downstream falló
            logger.error(SERVICE_UNAVAILABLE_MESSAGE_LOG, throwable.getMessage(), throwable);
            status = HttpStatus.SERVICE_UNAVAILABLE;
            responseBody.put(MESSAGE, SERVICE_UNAVAILABLE_MESSAGE);
        } else if (throwable instanceof TechnicalException) {
            logger.error(TECHINICAL_ERROR_MESSAGE_LOG, throwable.getMessage(), throwable);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            responseBody.put(MESSAGE, UNEXPECTED_ERROR_MESSAGE);
        } else {
            logger.error(UNEXPECTED_ERROR_MESSAGE_LOG, throwable);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            responseBody.put(MESSAGE, UNEXPECTED_ERROR_MESSAGE);
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(responseBody);
    }

    private List<Map<String, String>> toErrorList(Map<String, String> errors) {
        return errors.entrySet().stream()
                .map(entry -> {
                    Map<String, String> detail = new LinkedHashMap<>();
                    detail.put(FIELD, entry.getKey());
                    detail.put(MESSAGE, entry.getValue());
                    return detail;
                }).toList();
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::buildErrorResponse);
    }
}
