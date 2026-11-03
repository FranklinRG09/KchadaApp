package com.kchada.KchadaApp.post;

import com.google.cloud.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record PostDocument(
        String id,
        String ownerId,
        String title,
        String description,
        long priceCents,
        String category,
        ItemCondition condition,
        String department,
        String imageUrl,
        Double latitude,
        Double longitude,
        PostStatus status,
        long version,
        long likeCount,
        long commentCount,
        long saveCount,
        Date createdAt,
        Date updatedAt
) {

    public Map<String, Object> toMap() {

        Map<String, Object> data = new HashMap<>();

        data.put("id", id);
        data.put("ownerId", ownerId);
        data.put("title", title);
        data.put("description", description);
        data.put("priceCents", priceCents);
        data.put("category", category);
        data.put("condition", condition.name());
        data.put("department", department);
        data.put("imageUrl", imageUrl);
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("status", status.name());
        data.put("version", version);
        data.put("likeCount", likeCount);
        data.put("commentCount", commentCount);
        data.put("saveCount", saveCount);
        data.put("createdAt", createdAt);
        data.put("updatedAt", updatedAt);

        return data;
    }

    public static PostDocument fromSnapshot(
            DocumentSnapshot snapshot) {

        if (!snapshot.exists()) {
            return null;
        }

        return new PostDocument(
                snapshot.getId(),
                snapshot.getString("ownerId"),
                snapshot.getString("title"),
                snapshot.getString("description"),
                snapshot.getLong("priceCents"),
                snapshot.getString("category"),
                ItemCondition.valueOf(
                        snapshot.getString("condition")
                ),
                snapshot.getString("department"),
                snapshot.getString("imageUrl"),
                snapshot.getDouble("latitude"),
                snapshot.getDouble("longitude"),
                PostStatus.valueOf(
                        snapshot.getString("status")
                ),
                snapshot.getLong("version"),
                snapshot.getLong("likeCount"),
                snapshot.getLong("commentCount"),
                snapshot.getLong("saveCount"),
                snapshot.getDate("createdAt"),
                snapshot.getDate("updatedAt")
        );
    }

    public PostDocument withDetails(
            PostRequest request,
            long newPriceCents) {

        return new PostDocument(
                id,
                ownerId,
                request.title().trim(),
                request.description().trim(),
                newPriceCents,
                request.category().trim(),
                request.condition(),
                request.department().trim(),
                request.imageUrl(),
                request.latitude(),
                request.longitude(),
                status,
                version + 1,
                likeCount,
                commentCount,
                saveCount,
                createdAt,
                new Date()
        );
    }

    public PostDocument withStatus(PostStatus newStatus) {

        return new PostDocument(
                id,
                ownerId,
                title,
                description,
                priceCents,
                category,
                condition,
                department,
                imageUrl,
                latitude,
                longitude,
                newStatus,
                version + 1,
                likeCount,
                commentCount,
                saveCount,
                createdAt,
                new Date()
        );
    }
}