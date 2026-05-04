package com.example.chatbackend.service;

import com.example.chatbackend.model.Response;
import com.example.chatbackend.model.User;

public interface UserService {
    Response createUser(User user);
    boolean exists(String username);
}
