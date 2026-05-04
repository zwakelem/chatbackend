package com.example.chatbackend.model;

import lombok.Builder;

@Builder
public record Response (int status, String message) {}
