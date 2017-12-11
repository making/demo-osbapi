package com.example.demoosbapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

public class ServiceBrokerHandler {
    private static final Logger log = LoggerFactory.getLogger(ServiceBrokerHandler.class);
    private final Object catalog;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Mono<ServerResponse> badRequest = Mono.defer(() -> Mono
            .error(new ResponseStatusException(HttpStatus.BAD_REQUEST)));

    public ServiceBrokerHandler(Resource catalog) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream stream = catalog.getInputStream()) {
            this.catalog = yaml.load(stream);
        }
    }

    public RouterFunction<ServerResponse> routes() {
        final String serviceInstance = "/v2/service_instances/{instanceId}";
        final String serviceBindings = "/v2/service_instances/{instanceId}/service_bindings/{bindingId}";
        return route(GET("/v2/catalog"), this::catalog)
                .andRoute(PUT(serviceInstance), this::provisioning)
                .andRoute(PATCH(serviceInstance), this::update)
                .andRoute(GET(serviceInstance + "/last_operation"), this::lastOperation)
                .andRoute(DELETE(serviceInstance), this::deprovisioning)
                .andRoute(PUT(serviceBindings), this::bind)
                .andRoute(DELETE(serviceBindings), this::unbind)
                .filter(this::versionCheck);
    }

    Mono<ServerResponse> catalog(ServerRequest request) {
        return ok().syncBody(catalog);
    }

    Mono<ServerResponse> provisioning(ServerRequest request) {
        String instanceId = request.pathVariable("instanceId");
        log.info("Provisioning instanceId={}", instanceId);
        return request.bodyToMono(JsonNode.class) //
                .filter(this::validateServiceIdInBody) //
                .filter(this::validatePlanIdInBody) //
                .filter(this::validateGuidIdInBody) //
                .flatMap(r -> {
                    ObjectNode res = this.objectMapper.createObjectNode() //
                            .put("dashboard_url", "http://example.com");
                    return status(HttpStatus.CREATED).syncBody(res);
                }) //
                .switchIfEmpty(this.badRequest);
    }

    Mono<ServerResponse> update(ServerRequest request) {
        String instanceId = request.pathVariable("instanceId");
        log.info("Updating instanceId={}", instanceId);
        return request.bodyToMono(JsonNode.class) //
                .filter(this::validateServiceIdInBody) //
                .filter(this::validatePlanIdInBody) //
                .flatMap(r -> ok().syncBody(Collections.emptyMap())) //
                .switchIfEmpty(this.badRequest);
    }

    Mono<ServerResponse> deprovisioning(ServerRequest request) {
        String instanceId = request.pathVariable("instanceId");
        log.info("Deprovisioning instanceId={}", instanceId);
        if (!this.validateParameters(request)) {
            return this.badRequest;
        }
        return ok().syncBody(Collections.emptyMap());
    }

    Mono<ServerResponse> lastOperation(ServerRequest request) {
        return ok().syncBody(this.objectMapper.createObjectNode() //
                .put("state", "succeeded"));
    }

    Mono<ServerResponse> bind(ServerRequest request) {
        String instanceId = request.pathVariable("instanceId");
        String bindingId = request.pathVariable("bindingId");
        log.info("bind instanceId={}, bindingId={}", instanceId, bindingId);
        return request.bodyToMono(JsonNode.class) //
                .filter(this::validateServiceIdInBody) //
                .filter(this::validatePlanIdInBody) //
                .flatMap(r -> {
                    ObjectNode res = this.objectMapper.createObjectNode();
                    res.putObject("credentials") //
                            .put("username", UUID.randomUUID().toString()) //
                            .put("password", UUID.randomUUID().toString());
                    return status(HttpStatus.CREATED).syncBody(res);
                }) //
                .switchIfEmpty(this.badRequest);
    }

    Mono<ServerResponse> unbind(ServerRequest request) {
        String instanceId = request.pathVariable("instanceId");
        String bindingId = request.pathVariable("bindingId");
        log.info("unbind instanceId={}, bindingId={}", instanceId, bindingId);
        if (!this.validateParameters(request)) {
            return this.badRequest;
        }
        return ok().syncBody(Collections.emptyMap());
    }

    private boolean validateParameters(ServerRequest request) {
        return request.queryParam("plan_id").isPresent()
                && request.queryParam("service_id").isPresent();
    }

    private boolean validatePlanIdInBody(JsonNode node) {
        return node.has("plan_id") && node.get("plan_id").asText().length() == 36;
    }

    private boolean validateServiceIdInBody(JsonNode node) {
        return node.has("service_id") && node.get("service_id").asText().length() == 36;
    }

    private boolean validateGuidIdInBody(JsonNode node) {
        return node.has("organization_guid") && node.has("space_guid");
    }

    private Mono<ServerResponse> versionCheck(ServerRequest request,
                                              HandlerFunction<ServerResponse> function) {
        List<String> apiVersion = request.headers().header("X-Broker-API-Version");
        if (CollectionUtils.isEmpty(apiVersion)) {
            return status(HttpStatus.PRECONDITION_FAILED).build();
        }
        return function.handle(request);
    }
}