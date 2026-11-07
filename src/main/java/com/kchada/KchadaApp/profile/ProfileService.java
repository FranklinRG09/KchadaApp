package com.kchada.KchadaApp.profile;

import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.follow.FollowRepository;
import com.kchada.KchadaApp.user.UserDocument;
import com.kchada.KchadaApp.user.UserRepository;
import com.kchada.KchadaApp.user.UserStatus;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(
            ProfileRepository profileRepository,
            UserRepository userRepository,
            FollowRepository followRepository,
            PasswordEncoder passwordEncoder) {

        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileResponse getProfile(
            String userId,
            String viewerId) {

        UserDocument user = requireActiveUser(userId);

        ProfileDocument profile =
                profileRepository.findById(userId);

        FollowRepository.FollowStats stats =
                followRepository.stats(userId);

        boolean owner = userId.equals(viewerId);

        boolean following = viewerId != null
                && !owner
                && followRepository.isFollowing(
                        viewerId,
                        userId
                );

        long activePosts =
                profileRepository.countPosts(
                        userId,
                        "ACTIVE"
                );

        long soldPosts =
                profileRepository.countPosts(
                        userId,
                        "SOLD"
                );

        Long savedCount = owner
                ? profileRepository.countSaved(userId)
                : null;

        return new ProfileResponse(
                user.id(),
                user.fullName(),
                profile.bio(),
                profile.photoUrl(),
                profile.department(),
                stats.followersCount(),
                stats.followingCount(),
                activePosts,
                soldPosts,
                following,
                owner ? user.email() : null,
                savedCount
        );
    }

    public ProfileResponse update(
            String userId,
            UpdateProfileRequest request) {

        String fullName =
                request.fullName().trim();

        String bio = request.bio() == null
                ? ""
                : request.bio().trim();

        String department =
                request.department() == null
                        ? ""
                        : request.department().trim();

        String photoUrl =
                normalizePhotoUrl(request.photoUrl());

        ProfileDocument profile =
                new ProfileDocument(
                        userId,
                        bio,
                        photoUrl,
                        department
                );

        profileRepository.update(
                userId,
                fullName,
                profile
        );

        return getProfile(userId, userId);
    }

    public void changePassword(
            String userId,
            ChangePasswordRequest request) {

        if (!request.newPassword().equals(
                request.confirmPassword())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Las contraseñas nuevas no coinciden"
            );
        }

        if (request.newPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña es demasiado larga"
            );
        }

        UserDocument user = requireActiveUser(userId);

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.passwordHash())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña actual es incorrecta"
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.passwordHash())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe ser diferente"
            );
        }

        String newHash =
                passwordEncoder.encode(
                        request.newPassword()
                );

        boolean updated =
                profileRepository.changePassword(
                        userId,
                        user.passwordHash(),
                        newHash
                );

        if (!updated) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La contraseña fue modificada. Intenta nuevamente"
            );
        }
    }

    private UserDocument requireActiveUser(String userId) {

        UserDocument user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );

        if (user.status() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
            );
        }

        return user;
    }

    private String normalizePhotoUrl(String photoUrl) {

        if (photoUrl == null || photoUrl.isBlank()) {
            return "";
        }

        String value = photoUrl.trim();

        try {

            URI uri = URI.create(value);

            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {

                throw new IllegalArgumentException();
            }

            return value;

        } catch (IllegalArgumentException exception) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La fotografía debe utilizar una URL HTTPS válida"
            );
        }
    }
}