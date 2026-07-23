package co.com.pragma.api;

import co.com.pragma.api.constants.QueryParamConstants;
import co.com.pragma.api.dto.CapabilityExistenceInDto;
import co.com.pragma.api.dto.CapabilityExistenceOutDto;
import co.com.pragma.api.dto.CapabilityInDto;
import co.com.pragma.api.mapper.CapabilityDtoMapper;
import co.com.pragma.model.capability.query.CapabilityListQuery;
import co.com.pragma.usecase.checkcapabilitiesexistence.CheckCapabilitiesExistenceUseCase;
import co.com.pragma.usecase.listcapabilities.ListCapabilitiesUseCase;
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

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "10";
    private static final String DEFAULT_SORT_BY = "name";
    private static final String DEFAULT_SORT_DIRECTION = "asc";

    private final RegisterCapabilityUseCase registerCapabilityUseCase;
    private final ListCapabilitiesUseCase listCapabilitiesUseCase;
    private final CheckCapabilitiesExistenceUseCase checkCapabilitiesExistenceUseCase;
    private final CapabilityDtoMapper capabilityDtoMapper;

    @Override
    public Mono<ServerResponse> listenRegisterCapability(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CapabilityInDto.class)
                .defaultIfEmpty(new CapabilityInDto(null, null, null))
                .map(capabilityDtoMapper::toCapabilityCreateCommand)
                .flatMap(command -> registerCapabilityUseCase.execute(command)
                        .map(capability -> capabilityDtoMapper.toCapabilityOutDto(capability, command.technologyIds())))
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response));
    }

    @Override
    public Mono<ServerResponse> listenListCapabilities(ServerRequest serverRequest) {
        CapabilityListQuery query = new CapabilityListQuery(
                serverRequest.queryParam(QueryParamConstants.PAGE).orElse(DEFAULT_PAGE),
                serverRequest.queryParam(QueryParamConstants.SIZE).orElse(DEFAULT_SIZE),
                serverRequest.queryParam(QueryParamConstants.SORT_BY).orElse(DEFAULT_SORT_BY),
                serverRequest.queryParam(QueryParamConstants.SORT_DIRECTION).orElse(DEFAULT_SORT_DIRECTION));

        return listCapabilitiesUseCase.execute(query)
                .map(capabilityDtoMapper::toCapabilityPageOutDto)
                .flatMap(response -> ServerResponse.status(HttpStatus.OK).bodyValue(response));
    }

    @Override
    public Mono<ServerResponse> listenCheckCapabilitiesExistence(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CapabilityExistenceInDto.class)
                .defaultIfEmpty(new CapabilityExistenceInDto(null))
                .flatMap(dto -> checkCapabilitiesExistenceUseCase.execute(dto.capabilityIds()))
                .map(CapabilityExistenceOutDto::new)
                .flatMap(response -> ServerResponse.status(HttpStatus.OK).bodyValue(response));
    }
}
