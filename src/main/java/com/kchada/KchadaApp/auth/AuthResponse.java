package com.kchada.KchadaApp.auth;

public record AuthResponse(

        String token,
        String tokenType,
        long expiresIn,
        UserInfo user

) {

    public record UserInfo(

            String id,
            String fullName,
            String email,
            String role

    ) {
    }
}
