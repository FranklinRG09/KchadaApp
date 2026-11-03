package com.kchada.KchadaApp.post;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PostRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 100)
        String title,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 2000)
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01")
        @Digits(integer = 9, fraction = 2)
        BigDecimal price,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 80)
        String category,

        @NotNull(message = "La condición es obligatoria")
        ItemCondition condition,

        @NotBlank(message = "El departamento es obligatorio")
        String department,

        @Size(max = 2048)
        String imageUrl,

        Double latitude,

        Double longitude,

        Long baseVersion

) {
}