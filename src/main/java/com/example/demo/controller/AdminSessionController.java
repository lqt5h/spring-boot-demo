package com.example.demo.controller;

import com.example.demo.entity.UserSession;
import com.example.demo.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/sessions")
public class AdminSessionController {

    @Autowired
    private UserSessionRepository userSessionRepository;

    public static class SessionResponse {
        private Long id;
        private Long userId;
        private String refreshToken;
        private String status;
        private String createdAt;
        private String expiresAt;
        private String revokedAt;

        public SessionResponse(UserSession session) {
            this.id = session.getId();
            this.userId = session.getUser() != null ? session.getUser().getId() : null;
            this.refreshToken = session.getRefreshToken();
            this.status = session.getStatus() != null ? session.getStatus().name() : null;
            this.createdAt = session.getCreatedAt() != null ? session.getCreatedAt().toString() : null;
            this.expiresAt = session.getExpiresAt() != null ? session.getExpiresAt().toString() : null;
            this.revokedAt = session.getRevokedAt() != null ? session.getRevokedAt().toString() : null;
        }

        public Long getId() { return id; }
        public Long getUserId() { return userId; }
        public String getRefreshToken() { return refreshToken; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
        public String getExpiresAt() { return expiresAt; }
        public String getRevokedAt() { return revokedAt; }
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAllSessions() {
        List<UserSession> sessions = userSessionRepository.findAll();
        List<SessionResponse> result = sessions.stream()
                .map(SessionResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
