package com.kchada.KchadaApp.user;

import com.google.cloud.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record UserDocument(
        String id,
        String fullName,
        String email,
        String passwordHash,
        UserRole role,
        UserStatus status,
        long tokenVersion,
        Date createdAt,
        Date updatedAt
) {

    public Map<String, Object> toMap() {

        Map<String, Object> data = new HashMap<>();

        data.put("id", id);
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("passwordHash", passwordHash);
        data.put("role", role.name());
        data.put("status", status.name());
        data.put("tokenVersion", tokenVersion);
        data.put("createdAt", createdAt);
        data.put("updatedAt", updatedAt);

        return data;
    }

    public static UserDocument fromSnapshot(
            DocumentSnapshot snapshot) {

        if (!snapshot.exists()) {
            return null;
        }

        return new UserDocument(
                snapshot.getId(),
                snapshot.getString("fullName"),
                snapshot.getString("email"),
                snapshot.getString("passwordHash"),
                UserRole.valueOf(snapshot.getString("role")),
                UserStatus.valueOf(snapshot.getString("status")),
                snapshot.getLong("tokenVersion"),
                snapshot.getDate("createdAt"),
                snapshot.getDate("updatedAt")
        );
    }
}
