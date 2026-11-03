package com.kchada.KchadaApp.post;

import jakarta.validation.constraints.NotNull;

public record PostStatusRequest(

        @NotNull(message = "El estado es obligatorio")
        PostStatus status,

        Long baseVersion

) {
}
