package com.kchada.KchadaApp.chat;

import com.google.cloud.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record ChatResponse(

        String id,
        String postId,
        String buyerId,
        String sellerId,
        String partnerId,
        String lastMessage,
        String lastSenderId,
        long messageCount,
        long unreadCount,
        Date createdAt,
        Date updatedAt

) {

    public Map<String, Object> toMap() {

        Map<String, Object> data = new HashMap<>();

        data.put("postId", postId);
        data.put("buyerId", buyerId);
        data.put("sellerId", sellerId);
        data.put("partnerId", partnerId);
        data.put("lastMessage", lastMessage);
        data.put("lastSenderId", lastSenderId);
        data.put("messageCount", messageCount);
        data.put("unreadCount", unreadCount);
        data.put("createdAt", createdAt);
        data.put("updatedAt", updatedAt);

        return data;
    }

    public static ChatResponse fromSnapshot(
            DocumentSnapshot snapshot) {

        return new ChatResponse(
                snapshot.getId(),
                snapshot.getString("postId"),
                snapshot.getString("buyerId"),
                snapshot.getString("sellerId"),
                snapshot.getString("partnerId"),
                snapshot.getString("lastMessage"),
                snapshot.getString("lastSenderId"),
                snapshot.getLong("messageCount"),
                snapshot.getLong("unreadCount"),
                snapshot.getDate("createdAt"),
                snapshot.getDate("updatedAt")
        );
    }
}
