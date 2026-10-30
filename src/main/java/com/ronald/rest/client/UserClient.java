package com.ronald.rest.client;

import com.ronald.rest.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserClient {

    private static final String USERS_PATH = "/users";
    private static final String USER_BY_ID_PATH = "/users/{id}";

    private final RestClient restClient;

    public List<UserDto> findAll() {
        return restClient.get()
                .uri(USERS_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public UserDto findById(Long id) {
        return restClient.get()
                .uri(USER_BY_ID_PATH, id)
                .retrieve()
                .body(UserDto.class);
    }

    public UserDto create(UserDto user) {
        return restClient.post()
                .uri(USERS_PATH)
                .body(user)
                .retrieve()
                .body(UserDto.class);
    }

    public UserDto update(Long id, UserDto user) {
        return restClient.put()
                .uri(USER_BY_ID_PATH, id)
                .body(user)
                .retrieve()
                .body(UserDto.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri(USER_BY_ID_PATH, id)
                .retrieve()
                .toBodilessEntity();
    }

}
