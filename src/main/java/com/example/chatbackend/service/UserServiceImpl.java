package com.example.chatbackend.service;

import com.example.chatbackend.data.DataStore;
import com.example.chatbackend.model.Response;
import com.example.chatbackend.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DataStore dataStore;

    @Override
    public Response createUser(User user) {
        var createdUser = dataStore.addUser(user.username());
        var message = "";
        int statusCode;
        if(createdUser == null) {
            message = "User created";
            statusCode = HttpStatus.CREATED.value();
        } else {
            message = "User already exists";
            statusCode = HttpStatus.OK.value();
        }
        log.info("createUser::: users={}", dataStore.getUsers().toString());
        return Response.builder()
                .status(statusCode)
                .message(message)
                .build();
    }

    @Override
    public boolean exists(String username) {
        return dataStore.userExists(username);
    }
}
