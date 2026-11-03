package com.kchada.KchadaApp.interaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(

        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 500, message = "Máximo 500 caracteres")
        String text

) {
}
