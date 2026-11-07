package com.kchada.KchadaApp.chat;

public record ChatMessageSentEvent(

        String chatId,
        String messageId,
        String postId,
        String senderId,
        String recipientId

) {
}
