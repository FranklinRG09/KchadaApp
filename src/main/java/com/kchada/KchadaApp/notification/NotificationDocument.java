package com.kchada.KchadaApp.notification;

import com.google.cloud.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record NotificationDocument(

        String id,
        String recipientId,
        String actorId,
        NotificationType type,
        String title,
        String body,
        String postId,
        String chatId,
        boolean read,
        Date createdAt,
        Date readAt

) {

    public Map<String, Object> toMap() {

        Map<String, Object> data = new HashMap<>();

        data.put("recipientId", recipientId);
        data.put("actorId", actorId);
        data.put("type", type.name());
        data.put("title", title);
        data.put("body", body);
        data.put("postId", postId);
        data.put("chatId", chatId);
        data.put("read", read);
        data.put("createdAt", createdAt);
        data.put("readAt", readAt);

        return data;
    }

    public static NotificationDocument fromSnapshot(
            DocumentSnapshot snapshot) {

        if (!snapshot.exists()) {
            return null;
        }

        return new NotificationDocument(
                snapshot.getId(),
                snapshot.getString("recipientId"),
                snapshot.getString("actorId"),
                NotificationType.valueOf(
                        snapshot.getString("type")
                ),
                snapshot.getString("title"),
                snapshot.getString("body"),
                snapshot.getString("postId"),
                snapshot.getString("chatId"),
                Boolean.TRUE.equals(
                        snapshot.getBoolean("read")
                ),
                snapshot.getDate("createdAt"),
                snapshot.getDate("readAt")
        );
    }
}