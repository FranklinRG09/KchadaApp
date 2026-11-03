package com.kchada.KchadaApp.post;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            Authentication authentication,
            @Valid @RequestBody PostRequest request) {

        PostResponse response = postService.create(
                authentication.getName(),
                request
        );

        return ResponseEntity
                .created(
                        URI.create("/api/posts/" + response.id())
                )
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> findAll(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String after) {

        return ResponseEntity.ok(
                postService.findActive(limit, after)
        );
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<PostResponse>> findByOwner(
            @PathVariable String ownerId,
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(
                postService.findByOwner(ownerId, limit)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> findById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                postService.findById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> update(
            @PathVariable String id,
            Authentication authentication,
            @Valid @RequestBody PostRequest request) {

        return ResponseEntity.ok(
                postService.update(
                        id,
                        authentication.getName(),
                        request
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PostResponse> changeStatus(
            @PathVariable String id,
            Authentication authentication,
            @Valid @RequestBody PostStatusRequest request) {

        return ResponseEntity.ok(
                postService.changeStatus(
                        id,
                        authentication.getName(),
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestParam(required = false) Long baseVersion,
            Authentication authentication) {

        postService.delete(
                id,
                authentication.getName(),
                baseVersion
        );

        return ResponseEntity.noContent().build();
    }
}
