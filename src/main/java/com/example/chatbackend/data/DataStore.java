package com.example.chatbackend.data;

import com.example.chatbackend.model.Message;
import com.example.chatbackend.model.User;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Component
public class DataStore {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, List<Message>> messages = new HashMap<>();

    public User addUser(String username) {
        return users.put(username, new User(username));
    }

    public void addMessage(Message message, String sender) {
        messages.computeIfAbsent(sender, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(message);
    }

    public List<Message> getUserMessages(String username) {
        return messages.get(username);
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }
}
