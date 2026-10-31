package com.ronald.rest.client;

import com.ronald.rest.dto.UserDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserClientRetryTest {

    private static final AtomicInteger CALL_COUNT = new AtomicInteger();
    private static final HttpServer SERVER = startStubServer();

    @Autowired
    private UserClient userClient;

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("jsonplaceholder.base-url", () -> "http://localhost:" + SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stopStubServer() {
        SERVER.stop(0);
    }

    @Test
    void retriesOnBadGatewayAndEventuallySucceeds() {
        UserDto user = userClient.findById(1L);

        assertThat(user.getName()).isEqualTo("Leanne Graham");
        assertThat(CALL_COUNT.get()).isEqualTo(3);
    }

    private static HttpServer startStubServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/users/1", exchange -> {
                int attempt = CALL_COUNT.incrementAndGet();
                if (attempt < 3) {
                    exchange.sendResponseHeaders(502, -1);
                } else {
                    byte[] body = "{\"id\":1,\"name\":\"Leanne Graham\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
