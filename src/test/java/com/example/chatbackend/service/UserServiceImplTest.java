package com.example.chatbackend.service;

import com.example.chatbackend.data.DataStore;
import com.example.chatbackend.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private DataStore dataStore;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("test createUser(), user didn't exists")
    void createUser1() {
        var user = new User("user1");
        when(dataStore.addUser(anyString())).thenReturn(null);

        var result = userService.createUser(user);

        assertNotNull(result);
        assertEquals("User created", result.message());
        assertEquals(201, result.status());
        verify(dataStore, atLeastOnce()).addUser(anyString());
    }

    @Test
    @DisplayName("test createUser(), user already exists")
    void createUser2() {
        var user = new User("user1");
        when(dataStore.addUser(anyString())).thenReturn(user);

        var result = userService.createUser(user);

        assertNotNull(result);
        assertEquals(200, result.status());
        assertEquals("User already exists", result.message());
        verify(dataStore, atLeastOnce()).addUser(anyString());
    }
}
