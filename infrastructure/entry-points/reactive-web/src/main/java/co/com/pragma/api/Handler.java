package co.com.pragma.api;

import co.com.pragma.api.dto.CapabilityInDto;
import co.com.pragma.api.mapper.CapabilityDtoMapper;
import co.com.pragma.usecase.registercapability.RegisterCapabilityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler implements IHandlerDocs {

    private final RegisterCapabilityUseCase registerCapabilityUseCase;
    private final CapabilityDtoMapper capabilityDtoMapper;

    @Override
    public Mono<ServerResponse> listenRegisterCapability(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CapabilityInDto.class)
                .defaultIfEmpty(new CapabilityInDto(null, null, null))
                .map(capabilityDtoMapper::toCommand)
                .flatMap(command -> registerCapabilityUseCase.execute(command)
                        .map(capability -> capabilityDtoMapper.toResponse(capability, command.technologyIds())))
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response));
    }
}
