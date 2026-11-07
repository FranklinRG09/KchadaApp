package com.kchada.KchadaApp.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(

        @NotBlank(message = "La publicación es obligatoria")
        String postId

) {
}