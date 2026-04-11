package com.example.karatebdd.post;

import java.time.LocalDateTime;

public record PostResponse(Long id, String title, String content, Long authorId, String authorName, LocalDateTime createdAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.getCreatedAt()
        );
    }
}
