package co.com.pragma.api;

import co.com.pragma.api.dto.CapabilityInDto;
import co.com.pragma.api.dto.CapabilityOutDto;
import io.swagger.v3.oas.annotations.Operation;
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
                                    """)))
    })
    Mono<ServerResponse> listenRegisterCapability(ServerRequest serverRequest);
}
