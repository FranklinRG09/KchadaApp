package com.kchada.KchadaApp.post;

import java.math.BigDecimal;
import java.util.Date;

public record PostResponse(

        String id,
        String ownerId,
        String title,
        String description,
        BigDecimal price,
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

    public static PostResponse fromDocument(
            PostDocument post) {

        return new PostResponse(
                post.id(),
                post.ownerId(),
                post.title(),
                post.description(),
                BigDecimal.valueOf(
                        post.priceCents(),
                        2
                ),
                post.category(),
                post.condition(),
                post.department(),
                post.imageUrl(),
                post.latitude(),
                post.longitude(),
                post.status(),
                post.version(),
                post.likeCount(),
                post.commentCount(),
                post.saveCount(),
                post.createdAt(),
                post.updatedAt()
        );
    }
}
