package com.kchada.KchadaApp.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(

        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(
                max = 2000,
                message = "El mensaje supera los 2000 caracteres"
        )
        String text

) {
}