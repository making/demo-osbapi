package com.example.demoosbapi;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.ipc.netty.http.server.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public class DemoOsbapiApplication {
    private static RouterFunction<?> routes() {
        Resource catalog = new ClassPathResource("catalog.yml");
        try {
            return new ServiceBrokerHandler(catalog).routes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        long begin = System.currentTimeMillis();
        int port = Optional.ofNullable(System.getenv("PORT")) //
                .map(Integer::parseInt) //
                .orElse(8080);
        HttpServer httpServer = HttpServer.create("0.0.0.0", port);
        httpServer.startRouterAndAwait(routes -> {
            HttpHandler httpHandler = RouterFunctions.toHttpHandler(routes());
            routes.route(x -> true, new ReactorHttpHandlerAdapter(httpHandler));
        }, context -> {
            long elapsed = System.currentTimeMillis() - begin;
            LoggerFactory.getLogger(DemoOsbapiApplication.class).info("Started in {} seconds", elapsed / 1000.0);
        });
    }
}
