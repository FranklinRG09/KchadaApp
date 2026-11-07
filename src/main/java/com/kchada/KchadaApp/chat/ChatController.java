package com.kchada.KchadaApp.chat;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> openChat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {

        return ResponseEntity.status(
                HttpStatus.OK
        ).body(
                chatService.openChat(
                        authentication.getName(),
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<ChatResponse>> myChats(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        return ResponseEntity.ok(
                chatService.myChats(
                        authentication.getName(),
                        limit
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatResponse> getChat(
            @PathVariable String id,
            Authentication authentication) {

        return ResponseEntity.ok(
                chatService.getChat(
                        id,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> messages(
            @PathVariable String id,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) Long before,
            Authentication authentication) {

        return ResponseEntity.ok(
                chatService.messages(
                        id,
                        authentication.getName(),
                        limit,
                        before
                )
        );
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {

        return ResponseEntity.status(
                HttpStatus.CREATED
        ).body(
                chatService.sendMessage(
                        id,
                        authentication.getName(),
                        request
                )
        );
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ChatResponse> markRead(
            @PathVariable String id,
            Authentication authentication) {

        return ResponseEntity.ok(
                chatService.markRead(
                        id,
                        authentication.getName()
                )
        );
    }
}