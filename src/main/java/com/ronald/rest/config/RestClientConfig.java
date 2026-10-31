package com.ronald.rest.config;

import com.ronald.rest.client.exception.RetryableServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Configuration
public class RestClientConfig {

    private static final Set<HttpStatusCode> RETRYABLE_STATUSES = Set.of(
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.NETWORK_AUTHENTICATION_REQUIRED
    );

    @Value("${jsonplaceholder.base-url}")
    private String jsonPlaceholderBaseUrl;

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.baseUrl(jsonPlaceholderBaseUrl)
                .defaultStatusHandler(RETRYABLE_STATUSES::contains, (request, response) -> {
                    throw new RetryableServerException(response.getStatusCode(),
                            "Received retryable status %s from %s %s".formatted(
                                    response.getStatusCode(), request.getMethod(), request.getURI()));
                })
                .build();
    }

}
