package com.example.demo.service;

import com.example.demo.entity.Attempt;
import com.example.demo.entity.User;
import com.example.demo.repository.AttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final UserService userService;

    @Autowired
    public AttemptService(AttemptRepository attemptRepository, UserService userService) {
        this.attemptRepository = attemptRepository;
        this.userService = userService;
    }

    // Начать новую попытку
    public Attempt startAttempt(Long userId, Long quizId) {
        User user = userService.getUserById(userId);
        Attempt attempt = new Attempt();
        attempt.setUser(user);
        attempt.setQuizId(quizId);
        attempt.setDetails("Started at " + LocalDateTime.now());
        return attemptRepository.save(attempt);
    }

    // Завершить попытку
    public Attempt submitAttempt(Long attemptId, Map<Long, Long> answers) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found with id: " + attemptId));

        long score = calculateScore(answers);
        attempt.setScore(score);
        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setDetails("Submitted with score: " + score);

        return attemptRepository.save(attempt);
    }

    // Получить попытку по ID
    public Attempt getAttemptById(Long attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found with id: " + attemptId));
    }

    // Получить все попытки пользователя
    public List<Attempt> getUserAttempts(Long userId) {
        User user = userService.getUserById(userId);
        return attemptRepository.findByUser(user);
    }

    // Сохранить попытку
    public Attempt saveAttempt(Attempt attempt, Long userId) {
        User user = userService.getUserById(userId);
        attempt.setUser(user);
        return attemptRepository.save(attempt);
    }

    // Простейший подсчёт баллов
    private long calculateScore(Map<Long, Long> answers) {
        return answers.entrySet().stream()
                .filter(entry -> entry.getValue().equals(entry.getKey() + 1))
                .count();
    }
}
