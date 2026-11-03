package com.kchada.KchadaApp.post;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostExploreController {

    private final PostExploreService exploreService;

    public PostExploreController(
            PostExploreService exploreService) {

        this.exploreService = exploreService;
    }

    @GetMapping("/explore")
    public ResponseEntity<List<PostResponse>> explore(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(
                exploreService.explore(
                        category,
                        department,
                        limit
                )
        );
    }
}
