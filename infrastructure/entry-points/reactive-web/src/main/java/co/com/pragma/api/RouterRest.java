package co.com.pragma.api;

import co.com.pragma.api.constants.PathVariableConstants;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
            @RouterOperation(path = "/api/v1/capabilities", method = {
                    RequestMethod.POST }, beanClass = Handler.class, beanMethod = "listenRegisterCapability"),
            @RouterOperation(path = "/api/v1/capabilities", method = {
                    RequestMethod.GET }, beanClass = Handler.class, beanMethod = "listenListCapabilities"),
            @RouterOperation(path = "/api/v1/capabilities/existence-check", method = {
                    RequestMethod.POST }, beanClass = Handler.class, beanMethod = "listenCheckCapabilitiesExistence"),
            @RouterOperation(path = "/api/v1/bootcamp-capabilities", method = {
                    RequestMethod.POST }, beanClass = Handler.class, beanMethod = "listenLinkBootcampCapabilities"),
            @RouterOperation(path = "/api/v1/bootcamp-capabilities/{" + PathVariableConstants.BOOTCAMP_ID + "}", method = {
                    RequestMethod.DELETE }, beanClass = Handler.class, beanMethod = "listenDeleteBootcampCapabilities")
    })
    public RouterFunction<ServerResponse> capabilityRouterFunction(Handler handler) {
        return route(POST("/api/v1/capabilities"), handler::listenRegisterCapability)
                .andRoute(GET("/api/v1/capabilities"), handler::listenListCapabilities)
                .andRoute(POST("/api/v1/capabilities/existence-check"), handler::listenCheckCapabilitiesExistence)
                .andRoute(POST("/api/v1/bootcamp-capabilities"), handler::listenLinkBootcampCapabilities)
                .andRoute(DELETE("/api/v1/bootcamp-capabilities/{" + PathVariableConstants.BOOTCAMP_ID + "}"), handler::listenDeleteBootcampCapabilities);
    }
}
