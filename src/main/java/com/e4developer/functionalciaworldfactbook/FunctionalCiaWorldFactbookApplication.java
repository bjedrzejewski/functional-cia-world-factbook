package com.e4developer.functionalciaworldfactbook;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.server.HttpServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class FunctionalCiaWorldFactbookApplication {

    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        HttpHandler httpHandler = RouterFunctions.toHttpHandler(createRouterFunction());

        HttpServer
                .create("localhost", 8080)
                .newHandler(new ReactorHttpHandlerAdapter(httpHandler))
                .block();

        Thread.currentThread().join();
    }

    private static RouterFunction createRouterFunction() throws FileNotFoundException {
        RouterFunction<ServerResponse> routerFunction
                = route(GET("/"),
                request -> createStringResponse("Welcome to the CIA World Factbook. Check for directories with /directories"));
        routerFunction = routeWithDirectories(routerFunction);

        return routerFunction;
    }

    private static RouterFunction<ServerResponse> routeWithDirectories(RouterFunction<ServerResponse> routerFunction) throws FileNotFoundException {
        File file = ResourceUtils.getFile("classpath:factbook");
        String[] directories = file.list();
        routerFunction = routerFunction.andRoute(GET("/directories"),
                request -> createStringResponse(String.join(",", directories)));
        for(String directory : directories){
            File countriesDir = ResourceUtils.getFile("classpath:factbook/"+directory);
            String[] countries = countriesDir.list();
            routerFunction = routerFunction.andRoute(GET("/"+directory),
                    request -> createStringResponse(String.join(",", countries)));
            for(String country : countries){
                File countryFile = ResourceUtils.getFile("classpath:factbook/"+directory+"/"+country);
                Scanner scanner = new Scanner(countryFile, "UTF-8" );
                String fileText = scanner.useDelimiter("\\A").next();
                scanner.close();
                routerFunction = routerFunction.andRoute(GET("/"+directory+"/"+country),
                        request -> createStringResponse(fileText));
            }
        }
        return routerFunction;
    }

    private static Mono<ServerResponse> createStringResponse(String response){
        return ServerResponse.ok().body(Mono.just(response), String.class);
    }

}
