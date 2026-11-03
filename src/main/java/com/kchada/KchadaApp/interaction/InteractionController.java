package com.kchada.KchadaApp.interaction;

import com.kchada.KchadaApp.post.PostResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class InteractionController {

    private final InteractionService service;

    public InteractionController(InteractionService service) {
        this.service = service;
    }

    @PutMapping("/api/interactions/{postId}/like")
    public Map<String, Boolean> like(
            @PathVariable String postId,
            Authentication authentication) {

        return service.like(
                postId,
                authentication.getName(),
                true
        );
    }

    @DeleteMapping("/api/interactions/{postId}/like")
    public Map<String, Boolean> unlike(
            @PathVariable String postId,
            Authentication authentication) {

        return service.like(
                postId,
                authentication.getName(),
                false
        );
    }

    @PutMapping("/api/interactions/{postId}/save")
    public Map<String, Boolean> save(
            @PathVariable String postId,
            Authentication authentication) {

        return service.save(
                postId,
                authentication.getName(),
                true
        );
    }

    @DeleteMapping("/api/interactions/{postId}/save")
    public Map<String, Boolean> unsave(
            @PathVariable String postId,
            Authentication authentication) {

        return service.save(
                postId,
                authentication.getName(),
                false
        );
    }

    @GetMapping("/api/interactions/{postId}/me")
    public Map<String, Boolean> myState(
            @PathVariable String postId,
            Authentication authentication) {

        return service.myState(
                postId,
                authentication.getName()
        );
    }

    @GetMapping("/api/interactions/saved")
    public List<PostResponse> saved(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        return service.savedPosts(
                authentication.getName(),
                limit
        );
    }

    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentResponse> comments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "20") int limit) {

        return service.comments(postId, limit);
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> comment(
            @PathVariable String postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        return ResponseEntity.status(
                HttpStatus.CREATED
        ).body(
                service.comment(
                        postId,
                        authentication.getName(),
                        request
                )
        );
    }

    @DeleteMapping(
            "/api/posts/{postId}/comments/{commentId}"
    )
    public ResponseEntity<Void> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId,
            Authentication authentication) {

        service.deleteComment(
                postId,
                commentId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
