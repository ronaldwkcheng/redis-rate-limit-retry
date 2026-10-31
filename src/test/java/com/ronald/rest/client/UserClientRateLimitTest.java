package com.ronald.rest.client;

import com.sun.net.httpserver.HttpServer;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real Bucket4j/Redis-backed rate limiter end to end, against the
 * local Redis instance configured in application.yml.
 */
@SpringBootTest
class UserClientRateLimitTest {

    private static final byte[] BUCKET_KEY =
            "rate-limiter:jsonplaceholder-user-client".getBytes(StandardCharsets.UTF_8);

    private static final AtomicInteger CALL_COUNT = new AtomicInteger();
    private static final HttpServer SERVER = startStubServer();

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProxyManager<byte[]> bucketProxyManager;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("jsonplaceholder.base-url", () -> "http://localhost:" + SERVER.getAddress().getPort());
        registry.add("client.rate-limiter.capacity", () -> 1);
        registry.add("client.rate-limiter.refill-tokens", () -> 1);
        registry.add("client.rate-limiter.refill-duration-seconds", () -> 1);
    }

    @AfterAll
    static void stopStubServer() {
        SERVER.stop(0);
    }

    @BeforeEach
    void resetBucket() {
        bucketProxyManager.removeProxy(BUCKET_KEY);
        CALL_COUNT.set(0);
    }

    @Test
    void blocksUntilNextRefillWindowWhenRateLimitIsExceeded() {
        Instant firstCallStart = Instant.now();
        userClient.findById(1L);
        Duration firstCallDuration = Duration.between(firstCallStart, Instant.now());

        Instant secondCallStart = Instant.now();
        userClient.findById(1L);
        Duration secondCallDuration = Duration.between(secondCallStart, Instant.now());

        assertThat(firstCallDuration).isLessThan(Duration.ofMillis(300));
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
