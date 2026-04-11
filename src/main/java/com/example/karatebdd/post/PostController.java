package com.example.karatebdd.post;

import com.example.karatebdd.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(postService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(postService.findById(id)));
    }

    @GetMapping("/by-author/{authorId}")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getByAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(ApiResponse.ok(postService.findByAuthorId(authorId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(@Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(postService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
