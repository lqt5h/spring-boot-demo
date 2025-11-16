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
                        // Публичные эндпоинты
                        .requestMatchers("/", "/hello", "/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // Квизы
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/**")
                        .hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/quizzes")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**")
                        .hasRole("ADMIN")

                        // Попытки
                        .requestMatchers("/api/attempts/**")
                        .hasAnyRole("USER", "TEACHER", "ADMIN")


                        // Пользователи и прогресс
                        // ВАЖНО: без "**" посередине, только один сегмент перед progress
                        .requestMatchers(HttpMethod.GET, "/users/*/progress")
                        .hasAnyRole("USER", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/**")
                        .hasAnyRole("USER", "TEACHER", "ADMIN")

                        // Всё остальное — только аутентифицированным
                        .anyRequest().authenticated()
                )
                // Для H2-консоли
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
