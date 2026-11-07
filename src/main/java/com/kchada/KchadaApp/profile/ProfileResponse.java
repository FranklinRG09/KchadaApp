package com.kchada.KchadaApp.profile;

public record ProfileResponse(

        String userId,
        String fullName,
        String bio,
        String photoUrl,
        String department,

        long followersCount,
        long followingCount,
        long activePostCount,
        long soldPostCount,

        boolean following,

        String email,
        Long savedCount

) {
}