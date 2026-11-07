package com.kchada.KchadaApp.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String fullName,

        @Size(max = 500)
        String bio,

        @Size(max = 2048)
        String photoUrl,

        @Size(max = 80)
        String department

) {
}