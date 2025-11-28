package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import com.example.demo.entity.SessionStatus;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TokenService {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Value("${jwt.refresh-token-expiration:604800000}")  // 7 days in ms
    private long refreshTokenExpiration;

    /**
     * Создаёт пару токенов (access + refresh) и сохраняет сессию в БД
     */
    @Transactional
    public TokenPair createTokenPair(User user) {
        // Генерируем токены
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // Создаём сессию в БД
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
        UserSession session = new UserSession(user, refreshToken, expiresAt);
        userSessionRepository.save(session);

        return new TokenPair(accessToken, refreshToken, refreshTokenExpiration / 1000);
    }

    /**
     * Обновляет токены: валидирует refresh, отзывает старую сессию, создаёт новую
     */
    @Transactional
    public TokenPair refreshTokens(String oldRefreshToken) {
        // 1. Проверяем, что refresh-токен валиден
        if (!jwtTokenProvider.validateRefreshToken(oldRefreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        // 2. Проверяем сессию в БД
        Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshToken(oldRefreshToken);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Session not found");
        }

        UserSession session = sessionOpt.get();

        // 3. Проверяем статус сессии
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new RuntimeException("Session is no longer active");
        }

        // 4. Проверяем, не истекла ли сессия
        if (session.isExpired()) {
            session.setStatus(SessionStatus.EXPIRED);
            userSessionRepository.save(session);
            throw new RuntimeException("Session has expired");
        }

        // 5. Отзываем старую сессию
        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);

        // 6. Создаём новую пару токенов
        User user = session.getUser();
        return createTokenPair(user);
    }

    /**
     * Проверяет, что access-токен валиден и принадлежит пользователю
     */
    public boolean validateAccessToken(String token) {
        return jwtTokenProvider.validateAccessToken(token);
    }

    /**
     * Получает username из access-токена
     */
    public String getUsernameFromAccessToken(String token) {
        if (validateAccessToken(token)) {
            return jwtTokenProvider.getUsernameFromToken(token);
        }
        return null;
    }

    /**
     * Отзывает все сессии пользователя (logout)
     */
    @Transactional
    public void revokeAllSessions(User user) {
        userSessionRepository.findByUserAndStatus(user, SessionStatus.ACTIVE)
                .forEach(session -> {
                    session.setStatus(SessionStatus.REVOKED);
                    session.setRevokedAt(LocalDateTime.now());
                    userSessionRepository.save(session);
                });
    }

    // Inner class for token pair
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private long expiresIn;

        public TokenPair(String accessToken, String refreshToken, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public long getExpiresIn() {
            return expiresIn;
        }
    }
}