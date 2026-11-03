package com.kchada.KchadaApp.post;

public record PostChangedEvent(

        String postId,
        String ownerId,
        Action action

) {

    public enum Action {
        CREATED,
        UPDATED,
        SOLD,
        REACTIVATED,
        DELETED
    }
}