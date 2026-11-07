package com.kchada.KchadaApp.notification;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDocument>> mine(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String after,
            Authentication authentication) {

        return ResponseEntity.ok(
                service.findMine(
                        authentication.getName(),
                        limit,
                        after
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            Authentication authentication) {

        return ResponseEntity.ok(
                service.unreadCount(
                        authentication.getName()
                )
        );
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable String id,
            Authentication authentication) {

        service.markRead(
                authentication.getName(),
                id
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(
            Authentication authentication) {

        return ResponseEntity.ok(
                service.markAllRead(
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            Authentication authentication) {

        service.delete(
                authentication.getName(),
                id
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/devices")
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody DeviceTokenRequest request,
            Authentication authentication) {

        service.registerDevice(
                authentication.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(
            value = "/devices",
            method = RequestMethod.DELETE
    )
    public ResponseEntity<Void> unregisterDevice(
            @Valid @RequestBody DeviceTokenRequest request,
            Authentication authentication) {

        service.unregisterDevice(
                authentication.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }
}