package com.kchada.KchadaApp.common;

import java.time.Instant;

public record ApiError(

        int status,
        String error,
        String message,
        Instant timestamp

) {
}