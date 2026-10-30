package com.ronald.rest.service;

import com.ronald.rest.client.UserClient;
import com.ronald.rest.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserClient userClient;

    public List<UserDto> getAllUsers() {
        return userClient.findAll();
    }

    public UserDto getUserById(Long id) {
        return userClient.findById(id);
    }

    public UserDto createUser(UserDto user) {
        return userClient.create(user);
    }

    public UserDto updateUser(Long id, UserDto user) {
        return userClient.update(id, user);
    }

    public void deleteUser(Long id) {
        userClient.delete(id);
    }

}
