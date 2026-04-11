package com.example.karatebdd.post;

import com.example.karatebdd.user.User;
import com.example.karatebdd.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public List<PostResponse> findAll() {
        return postRepository.findAll().stream()
                .map(PostResponse::from)
                .toList();
    }

    public PostResponse findById(Long id) {
        return postRepository.findById(id)
                .map(PostResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Post not found with id: " + id));
    }

    public List<PostResponse> findByAuthorId(Long authorId) {
        if (!userRepository.existsById(authorId)) {
            throw new NoSuchElementException("User not found with id: " + authorId);
        }
        return postRepository.findAllByAuthorId(authorId).stream()
                .map(PostResponse::from)
                .toList();
    }

    @Transactional
    public PostResponse create(CreatePostRequest request) {
        User author = userRepository.findById(request.authorId())
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + request.authorId()));
        Post post = new Post(request.title(), request.content(), author);
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional
    public PostResponse update(Long id, UpdatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found with id: " + id));
        post.update(request.title(), request.content());
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new NoSuchElementException("Post not found with id: " + id);
        }
        postRepository.deleteById(id);
    }
}
