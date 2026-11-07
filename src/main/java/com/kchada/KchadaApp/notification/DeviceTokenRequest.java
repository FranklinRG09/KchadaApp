package com.kchada.KchadaApp.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRequest(

        @NotBlank(message = "El token FCM es obligatorio")
        @Size(max = 4096)
        String token

) {
}
