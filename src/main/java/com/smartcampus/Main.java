package com.smartcampus;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.mapper.GlobalExceptionMapper;
import com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import java.io.IOException;
import java.net.URI;

public class Main {

    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws IOException, InterruptedException {
        ResourceConfig config = new ResourceConfig();
        config.register(DiscoveryResource.class);
        config.register(RoomResource.class);
        config.register(SensorResource.class);
        config.register(RoomNotEmptyExceptionMapper.class);
        config.register(LinkedResourceNotFoundExceptionMapper.class);
        config.register(SensorUnavailableExceptionMapper.class);
        config.register(GlobalExceptionMapper.class);
        config.register(LoggingFilter.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        System.out.println("==============================================");
        System.out.println(" Smart Campus API - RUNNING");
        System.out.println(" Discovery : http://localhost:8080/api/v1/");
        System.out.println(" Rooms     : http://localhost:8080/api/v1/rooms");
        System.out.println(" Sensors   : http://localhost:8080/api/v1/sensors");
        System.out.println(" Press CTRL+C to stop.");
        System.out.println("==============================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server stopped.");
            server.stop();
        }));
        Thread.currentThread().join();
    }
}
