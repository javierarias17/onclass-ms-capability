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

    private static final String CAPABILITIES_PATH = "/api/v1/capabilities";
    private static final String CAPABILITIES_EXISTENCE_CHECK_PATH = CAPABILITIES_PATH + "/existence-check";
    private static final String BOOTCAMP_CAPABILITIES_PATH = "/api/v1/bootcamp-capabilities";
    private static final String BOOTCAMP_CAPABILITIES_BY_ID_PATH = BOOTCAMP_CAPABILITIES_PATH
            + "/{" + PathVariableConstants.BOOTCAMP_ID + "}";
    private static final String BOOTCAMP_CAPABILITIES_BY_BOOTCAMP_IDS_PATH = BOOTCAMP_CAPABILITIES_PATH
            + "/by-bootcamp-ids";
    private static final String BOOTCAMP_CAPABILITIES_CASCADE_PATH = BOOTCAMP_CAPABILITIES_BY_ID_PATH + "/cascade";

    @Bean
    @RouterOperations({
            @RouterOperation(path = CAPABILITIES_PATH, method = {
                    RequestMethod.POST }, beanClass = Handler.class, beanMethod = "listenRegisterCapability"),
            @RouterOperation(path = CAPABILITIES_PATH, method = {
                    RequestMethod.GET }, beanClass = Handler.class, beanMethod = "listenListCapabilities"),
            @RouterOperation(path = CAPABILITIES_EXISTENCE_CHECK_PATH, method = {
                    RequestMethod.POST }, beanClass = Handler.class, beanMethod = "listenCheckCapabilitiesExistence"),
            @RouterOperation(path = BOOTCAMP_CAPABILITIES_PATH, method = {
                    RequestMethod.POST }, beanClass = Handler.class, beanMethod = "listenLinkBootcampCapabilities"),
            @RouterOperation(path = BOOTCAMP_CAPABILITIES_BY_ID_PATH, method = {
                    RequestMethod.DELETE }, beanClass = Handler.class, beanMethod = "listenDeleteBootcampCapabilities"),
            @RouterOperation(path = BOOTCAMP_CAPABILITIES_BY_BOOTCAMP_IDS_PATH, method = {
                    RequestMethod.POST }, beanClass = Handler.class, beanMethod = "listenFindCapabilitiesByBootcampIds"),
            @RouterOperation(path = BOOTCAMP_CAPABILITIES_CASCADE_PATH, method = {
                    RequestMethod.DELETE }, beanClass = Handler.class, beanMethod = "listenDeleteOrphanedCapabilitiesForBootcamp")
    })
    public RouterFunction<ServerResponse> capabilityRouterFunction(Handler handler) {
        return route(POST(CAPABILITIES_PATH), handler::listenRegisterCapability)
                .andRoute(GET(CAPABILITIES_PATH), handler::listenListCapabilities)
                .andRoute(POST(CAPABILITIES_EXISTENCE_CHECK_PATH), handler::listenCheckCapabilitiesExistence)
                .andRoute(POST(BOOTCAMP_CAPABILITIES_PATH), handler::listenLinkBootcampCapabilities)
                .andRoute(DELETE(BOOTCAMP_CAPABILITIES_BY_ID_PATH), handler::listenDeleteBootcampCapabilities)
                .andRoute(POST(BOOTCAMP_CAPABILITIES_BY_BOOTCAMP_IDS_PATH), handler::listenFindCapabilitiesByBootcampIds)
                .andRoute(DELETE(BOOTCAMP_CAPABILITIES_CASCADE_PATH), handler::listenDeleteOrphanedCapabilitiesForBootcamp);
    }
}
