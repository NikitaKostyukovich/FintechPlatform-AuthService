package com.nikita.authservice.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class FakeUserService {

    private final HttpServer server;
    private final List<String> createRequests = new CopyOnWriteArrayList<>();
    private final List<Long> deletedIds = new CopyOnWriteArrayList<>();
    private volatile int createStatus;
    private volatile String createBody;

    public FakeUserService() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start fake User Service", e);
        }
        server.createContext("/api/users", this::handle);
        server.start();
        reset();
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void reset() {
        createStatus = 201;
        createBody = """
            {
                "id": 100,
                "name": "Ivan",
                "surname": "Ivanov",
                "birthDate": "2000-01-15",
                "email": "ivan@example.com",
                "active": true
            }
            """;
        createRequests.clear();
        deletedIds.clear();
    }

    public void respondToCreateWith(int status, String body) {
        this.createStatus = status;
        this.createBody = body;
    }

    public List<String> createRequests() {
        return createRequests;
    }

    public List<Long> deletedIds() {
        return deletedIds;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equals(method) && "/api/users".equals(path)) {
                createRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                send(exchange, createStatus, createBody);
            } else if ("DELETE".equals(method) && path.startsWith("/api/users/")) {
                deletedIds.add(Long.parseLong(path.substring("/api/users/".length())));
                exchange.sendResponseHeaders(204, -1);
            } else {
                send(exchange, 404, "{}");
            }
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}