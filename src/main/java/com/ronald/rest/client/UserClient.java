package com.ronald.rest.client;

import com.ronald.rest.client.exception.RetryableServerException;
import com.ronald.rest.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserClient {

    private static final String USERS_PATH = "/users";
    private static final String USER_BY_ID_PATH = "/users/{id}";

    private final RestClient restClient;

    @Retryable(retryFor = RetryableServerException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.initial-delay-ms}",
                    multiplierExpression = "${client.retry.multiplier}"))
    public List<UserDto> findAll() {
        return restClient.get()
                .uri(USERS_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Retryable(retryFor = RetryableServerException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.initial-delay-ms}",
                    multiplierExpression = "${client.retry.multiplier}"))
    public UserDto findById(Long id) {
        return restClient.get()
                .uri(USER_BY_ID_PATH, id)
                .retrieve()
                .body(UserDto.class);
    }

    @Retryable(retryFor = RetryableServerException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.initial-delay-ms}",
                    multiplierExpression = "${client.retry.multiplier}"))
    public UserDto create(UserDto user) {
        return restClient.post()
                .uri(USERS_PATH)
                .body(user)
                .retrieve()
                .body(UserDto.class);
    }

    @Retryable(retryFor = RetryableServerException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.initial-delay-ms}",
                    multiplierExpression = "${client.retry.multiplier}"))
    public UserDto update(Long id, UserDto user) {
        return restClient.put()
                .uri(USER_BY_ID_PATH, id)
                .body(user)
                .retrieve()
                .body(UserDto.class);
    }

    @Retryable(retryFor = RetryableServerException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.initial-delay-ms}",
                    multiplierExpression = "${client.retry.multiplier}"))
    public void delete(Long id) {
        restClient.delete()
                .uri(USER_BY_ID_PATH, id)
                .retrieve()
                .toBodilessEntity();
    }

}
