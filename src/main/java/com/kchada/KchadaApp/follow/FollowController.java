package com.kchada.KchadaApp.follow;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService service;

    public FollowController(FollowService service) {
        this.service = service;
    }

    @PutMapping("/{userId}")
    public Map<String, Boolean> follow(
            @PathVariable String userId,
            Authentication authentication) {

        return service.follow(
                authentication.getName(),
                userId,
                true
        );
    }

    @DeleteMapping("/{userId}")
    public Map<String, Boolean> unfollow(
            @PathVariable String userId,
            Authentication authentication) {

        return service.follow(
                authentication.getName(),
                userId,
                false
        );
    }

    @GetMapping("/{userId}/me")
    public Map<String, Boolean> myState(
            @PathVariable String userId,
            Authentication authentication) {

        return service.myState(
                authentication.getName(),
                userId
        );
    }

    @GetMapping("/{userId}/stats")
    public FollowRepository.FollowStats stats(
            @PathVariable String userId) {

        return service.stats(userId);
    }

    @GetMapping("/{userId}/followers")
    public List<String> followers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {

        return service.followers(userId, limit);
    }

    @GetMapping("/{userId}/following")
    public List<String> following(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {

        return service.following(userId, limit);
    }
}
