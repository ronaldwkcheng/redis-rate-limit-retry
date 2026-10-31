package com.ronald.rest.client;

import com.redis.testcontainers.RedisContainer;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real Bucket4j/Redis-backed rate limiter end to end, against a
 * disposable Redis container so the test is isolated from any locally running
 * Redis instance. Requires Docker to be available.
 */
@SpringBootTest
@Testcontainers
class UserClientRateLimitTest {

    @Container
    static final RedisContainer REDIS =
            new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

    private static final AtomicInteger CALL_COUNT = new AtomicInteger();
    private static final HttpServer SERVER = startStubServer();

    @Autowired
    private UserClient userClient;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("jsonplaceholder.base-url", () -> "http://localhost:" + SERVER.getAddress().getPort());
        registry.add("spring.data.redis.host", REDIS::getRedisHost);
        registry.add("spring.data.redis.port", REDIS::getRedisPort);
        registry.add("client.rate-limiter.capacity", () -> 1);
        registry.add("client.rate-limiter.refill-tokens", () -> 1);
        registry.add("client.rate-limiter.refill-duration-seconds", () -> 1);
    }

    @AfterAll
    static void stopStubServer() {
        SERVER.stop(0);
    }

    @Test
    void blocksUntilNextRefillWindowWhenRateLimitIsExceeded() {
        Instant firstCallStart = Instant.now();
        userClient.findById(1L);
        Duration firstCallDuration = Duration.between(firstCallStart, Instant.now());

        Instant secondCallStart = Instant.now();
        userClient.findById(1L);
        Duration secondCallDuration = Duration.between(secondCallStart, Instant.now());

        assertThat(firstCallDuration).isLessThan(Duration.ofMillis(600));
        assertThat(secondCallDuration).isGreaterThanOrEqualTo(Duration.ofMillis(700));
        assertThat(CALL_COUNT.get()).isEqualTo(2);
    }

    private static HttpServer startStubServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/users/1", exchange -> {
                CALL_COUNT.incrementAndGet();
                byte[] body = "{\"id\":1,\"name\":\"Leanne Graham\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
