package com.kchada.KchadaApp.security;

import com.kchada.KchadaApp.user.UserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {

        return (request, response, exception) -> {

            response.setStatus(401);
            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    "{\"status\":401,"
                    + "\"error\":\"Unauthorized\","
                    + "\"message\":\"Autenticación requerida o inválida\"}"
            );
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository,
            AuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userRepository,
                        authenticationEntryPoint
                );

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions ->
                        exceptions
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        (request, response, exception) -> {
                                            response.setStatus(403);
                                            response.setContentType(
                                                    MediaType.APPLICATION_JSON_VALUE
                                            );
                                            response.setCharacterEncoding("UTF-8");
                                            response.getWriter().write(
                                                    "{\"status\":403,"
                                                    + "\"error\":\"Forbidden\","
                                                    + "\"message\":\"Acceso denegado\"}"
                                            );
                                        }
                                )
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/posts",
                                "/api/posts/**",
                                "/api/departments",
                                "/api/departments/**",
                                "/api/profiles/*",
                                "/api/profiles/*/posts"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}