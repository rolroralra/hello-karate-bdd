package com.example.karatebdd.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePostRequest(
        @NotBlank(message = "Title is required") String title,
        String content,
        @NotNull(message = "Author ID is required") Long authorId
) {}
