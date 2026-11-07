package com.kchada.KchadaApp.chat;

import java.util.Date;

public record MessageResponse(

        String id,
        String chatId,
        String senderId,
        String text,
        long sequence,
        boolean read,
        Date createdAt

) {
}