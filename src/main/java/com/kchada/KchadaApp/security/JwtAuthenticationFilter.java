package com.kchada.KchadaApp.security;

import com.kchada.KchadaApp.user.UserDocument;
import com.kchada.KchadaApp.user.UserRepository;
import com.kchada.KchadaApp.user.UserStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            AuthenticationEntryPoint authenticationEntryPoint) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        String path = request.getServletPath();
        String method = request.getMethod();

        return method.equals(HttpMethod.POST.name())
                && (
                    path.equals("/api/auth/login")
                    || path.equals("/api/auth/register")
                );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {

            Claims claims = jwtService.validateToken(token);

            String userId = claims.getSubject();

            Object versionClaim = claims.get("ver");

            if (userId == null
                    || !(versionClaim instanceof Number)) {

                throw new BadCredentialsException(
                        "Token inválido"
                );
            }

            long tokenVersion =
                    ((Number) versionClaim).longValue();

            UserDocument user = userRepository
                    .findById(userId)
                    .orElseThrow(() ->
                            new BadCredentialsException(
                                    "Usuario no encontrado"
                            )
                    );

            if (user.status() != UserStatus.ACTIVE
                    || user.tokenVersion() != tokenVersion) {

                throw new BadCredentialsException(
                        "Sesión inválida"
                );
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + user.role().name()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.id(),
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (JwtException
                | IllegalArgumentException
                | BadCredentialsException exception) {

            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(
                            "Token inválido o sesión cerrada"
                    )
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
