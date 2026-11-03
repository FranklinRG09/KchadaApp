package com.kchada.KchadaApp.auth;

import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.security.JwtService;
import com.kchada.KchadaApp.user.UserDocument;
import com.kchada.KchadaApp.user.UserRepository;
import com.kchada.KchadaApp.user.UserRole;
import com.kchada.KchadaApp.user.UserStatus;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        String fullName = request.fullName().trim();

        String email = normalizeEmail(request.email());

        if (!request.password().equals(
                request.confirmPassword())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Las contraseñas no coinciden"
            );
        }

        if (request.password()
                .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña es demasiado larga"
            );
        }

        Date now = new Date();

        UserDocument user = new UserDocument(
                UUID.randomUUID().toString(),
                fullName,
                email,
                passwordEncoder.encode(request.password()),
                UserRole.USER,
                UserStatus.ACTIVE,
                0L,
                now,
                now
        );

        boolean created = userRepository.create(user);

        if (!created) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
            );
        }

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        UserDocument user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.UNAUTHORIZED,
                                "Correo o contraseña incorrectos"
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.passwordHash())) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos"
            );
        }

        if (user.status() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta no está activa"
            );
        }

        return buildResponse(user);
    }

    public void logout(String userId) {
        userRepository.revokeSessions(userId);
    }

    public AuthResponse.UserInfo getCurrentUser(String userId) {

        UserDocument user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );

        return toUserInfo(user);
    }

    private AuthResponse buildResponse(UserDocument user) {

        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                toUserInfo(user)
        );
    }

    private AuthResponse.UserInfo toUserInfo(
            UserDocument user) {

        return new AuthResponse.UserInfo(
                user.id(),
                user.fullName(),
                user.email(),
                user.role().name()
        );
    }

    private String normalizeEmail(String email) {

        return email.trim().toLowerCase(Locale.ROOT);
    }
}