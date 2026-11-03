package com.kchada.KchadaApp.interaction;

import java.util.Date;

public record CommentResponse(

        String id,
        String postId,
        String authorId,
        String text,
        Date createdAt

) {
}
