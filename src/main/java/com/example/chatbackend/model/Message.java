package com.example.chatbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(
        String sender,
        LocalDateTime timestamp,
        String message
) {}
