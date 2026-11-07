package com.kchada.KchadaApp.profile;

import com.kchada.KchadaApp.post.PostResponse;
import com.kchada.KchadaApp.post.PostService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final PostService postService;

    public ProfileController(
            ProfileService profileService,
            PostService postService) {

        this.profileService = profileService;
        this.postService = postService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> me(
            Authentication authentication) {

        return ResponseEntity.ok(
                profileService.getProfile(
                        authentication.getName(),
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> profile(
            @PathVariable String userId,
            Authentication authentication) {

        return ResponseEntity.ok(
                profileService.getProfile(
                        userId,
                        viewerId(authentication)
                )
        );
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> update(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                profileService.update(
                        authentication.getName(),
                        request
                )
        );
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        profileService.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/posts")
    public ResponseEntity<List<PostResponse>> posts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {

        profileService.getProfile(userId, null);

        return ResponseEntity.ok(
                postService.findByOwner(
                        userId,
                        limit
                )
        );
    }

    private String viewerId(Authentication authentication) {

        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken) {

            return null;
        }

        return authentication.getName();
    }
}