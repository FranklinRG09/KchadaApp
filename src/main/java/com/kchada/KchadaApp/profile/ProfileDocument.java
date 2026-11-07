package com.kchada.KchadaApp.profile;

import com.google.cloud.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public record ProfileDocument(

        String userId,
        String bio,
        String photoUrl,
        String department

) {

    public Map<String, Object> toMap() {

        Map<String, Object> data = new HashMap<>();

        data.put("userId", userId);
        data.put("bio", bio);
        data.put("photoUrl", photoUrl);
        data.put("department", department);

        return data;
    }

    public static ProfileDocument fromSnapshot(
            String userId,
            DocumentSnapshot snapshot) {

        if (!snapshot.exists()) {
            return new ProfileDocument(
                    userId,
                    "",
                    "",
                    ""
            );
        }

        return new ProfileDocument(
                userId,
                value(snapshot.getString("bio")),
                value(snapshot.getString("photoUrl")),
                value(snapshot.getString("department"))
        );
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
