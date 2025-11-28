package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import com.example.demo.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshTokenAndStatus(String refreshToken, SessionStatus status);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    List<UserSession> findByUserAndStatus(User user, SessionStatus status);

    List<UserSession> findByUser(User user);

    void deleteByUser(User user);
}