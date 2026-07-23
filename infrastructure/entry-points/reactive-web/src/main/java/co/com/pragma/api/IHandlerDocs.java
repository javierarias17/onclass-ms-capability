package co.com.pragma.api;

import co.com.pragma.api.constants.QueryParamConstants;
import co.com.pragma.api.dto.CapabilityExistenceInDto;
import co.com.pragma.api.dto.CapabilityExistenceOutDto;
import co.com.pragma.api.dto.CapabilityInDto;
import co.com.pragma.api.dto.CapabilityOutDto;
import co.com.pragma.api.dto.CapabilityPageOutDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface IHandlerDocs {

    @Operation(
            operationId = "listenRegisterCapability",
            summary = "Register a capability",
            description = "Creates a new capability with a unique name and between 3 and 20 existing technology ids.",
            tags = { "Capabilities" },
            requestBody = @RequestBody(
                    description = "Input data",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CapabilityInDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "Backend",
                                      "description": "Backend development capability",
                                      "technologyIds": [1, 2, 3]
                                    }
                                    """))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CapabilityOutDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "name": "Backend",
                                      "description": "Backend development capability",
                                      "technologyIds": [1, 2, 3]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Invalid input", value = """
                                            {
                                              "message": "Business validation failed",
                                              "errors": [
                                                {
                                                  "field": "name",
                                                  "message": "Capability name is required"
                                                },
                                                {
                                                  "field": "technologyIds",
                                                  "message": "Capability must have at least 3 technologies"
                                                }
                                              ]
                                            }
                                            """),
                                    @ExampleObject(name = "Technologies not found", value = """
                                            {
                                              "message": "Business validation failed",
                                              "errors": [
                                                {
                                                  "field": "technologyIds",
                                                  "message": "The following technology ids do not exist: [99]"
                                                }
                                              ]
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "409", description = "Conflict",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Business validation failed",
                                      "errors": [
                                        {
                                          "field": "name",
                                          "message": "Capability name already exists"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "An unexpected error occurred. Please contact the administrator."
                                    }
                                    """))),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "The service is temporarily unavailable. Please try again shortly."
                                    }
                                    """)))
    })
    Mono<ServerResponse> listenRegisterCapability(ServerRequest serverRequest);

    @Operation(
            operationId = "listenListCapabilities",
            summary = "List capabilities",
            description = "Returns a paginated list of capabilities, each with its associated technologies (id and name only).",
            tags = { "Capabilities" },
            parameters = {
                    @Parameter(name = QueryParamConstants.PAGE, in = ParameterIn.QUERY, required = false,
                            schema = @Schema(type = "integer", defaultValue = "0")),
                    @Parameter(name = QueryParamConstants.SIZE, in = ParameterIn.QUERY, required = false,
                            schema = @Schema(type = "integer", defaultValue = "10")),
                    @Parameter(name = QueryParamConstants.SORT_BY, in = ParameterIn.QUERY, required = false,
                            schema = @Schema(type = "string", allowableValues = { "name", "technologyCount" }, defaultValue = "name")),
                    @Parameter(name = QueryParamConstants.SORT_DIRECTION, in = ParameterIn.QUERY, required = false,
                            schema = @Schema(type = "string", allowableValues = { "asc", "desc" }, defaultValue = "asc"))
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CapabilityPageOutDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "content": [
                                        {
                                          "id": 1,
                                          "name": "Backend",
                                          "description": "Backend development capability",
                                          "technologies": [
                                            { "id": 10, "name": "Java" },
                                            { "id": 11, "name": "Spring" }
                                          ]
                                        }
                                      ],
                                      "page": 0,
                                      "size": 10,
                                      "totalElements": 1,
                                      "totalPages": 1
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Business validation failed",
                                      "errors": [
                                        {
                                          "field": "sortBy",
                                          "message": "Sort field must be one of: name, technologyCount"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "An unexpected error occurred. Please contact the administrator."
                                    }
                                    """))),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "The service is temporarily unavailable. Please try again shortly."
                                    }
                                    """)))
    })
    Mono<ServerResponse> listenListCapabilities(ServerRequest serverRequest);

    @Operation(
            operationId = "listenCheckCapabilitiesExistence",
            summary = "Check capabilities existence",
            description = "Given a list of capability ids, returns the ids that do not exist. An empty list means all of them exist.",
            tags = { "Capabilities" },
            requestBody = @RequestBody(
                    description = "Input data",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CapabilityExistenceInDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "capabilityIds": [1, 2, 3]
                                    }
                                    """))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CapabilityExistenceOutDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "missingIds": [3]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Business validation failed",
                                      "errors": [
                                        {
                                          "field": "capabilityIds",
                                          "message": "Capability ids list is required and must not be empty"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "An unexpected error occurred. Please contact the administrator."
                                    }
                                    """)))
    })
    Mono<ServerResponse> listenCheckCapabilitiesExistence(ServerRequest serverRequest);
}
