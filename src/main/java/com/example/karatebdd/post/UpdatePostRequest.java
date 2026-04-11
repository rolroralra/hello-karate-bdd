package com.example.karatebdd.post;

import jakarta.validation.constraints.NotBlank;

public record UpdatePostRequest(
        @NotBlank(message = "Title is required") String title,
        String content
) {}
