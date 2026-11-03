package com.kchada.KchadaApp.common;

public record SocialEvent(

        Type type,
        String actorId,
        String recipientId,
        String postId

) {

    public enum Type {
        POST_LIKED,
        POST_COMMENTED,
        USER_FOLLOWED
    }
}
