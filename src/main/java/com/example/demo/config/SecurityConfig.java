package com.example.demo.config;

import com.example.demo.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers("/", "/hello", "/auth/register", "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // ===== TEACHER endpoints =====
                        .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN")

                        // Quizzes
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/**").hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/quizzes").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**").hasRole("ADMIN")

                        // Questions
                        .requestMatchers(HttpMethod.GET, "/api/questions/**").hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/questions/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/questions/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/questions/**").hasAnyRole("TEACHER", "ADMIN")

                        // Answer options
                        .requestMatchers(HttpMethod.GET, "/api/answer-options/**").hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/answer-options/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/answer-options/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/answer-options/**").hasAnyRole("TEACHER", "ADMIN")

                        // Attempts
                        .requestMatchers(HttpMethod.PUT, "/api/attempts/*").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/attempts/*/answers").hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers("/api/attempts/**").hasAnyRole("USER", "TEACHER", "ADMIN")

                        // Users
                        .requestMatchers(HttpMethod.POST, "/api/users/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/*/progress").hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyRole("USER", "TEACHER", "ADMIN")

                        // Everything else
                        .anyRequest().authenticated()

                )

                // H2 console
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
