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
    public AttemptService(AttemptRepository attemptRepository,
                          UserService userService) {
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

    // Завершить попытку и посчитать баллы
    public Attempt submitAttempt(Long attemptId, Map<String, Object> answers) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() ->
                        new RuntimeException("Attempt not found with id: " + attemptId));

        long score = calculateScore(answers);
        attempt.setScore(score);
        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setDetails("Submitted with score: " + score);

        return attemptRepository.save(attempt);
    }

    // Простая логика подсчёта баллов
    private long calculateScore(Map<String, Object> answers) {
        return answers.entrySet().stream()
                .filter(entry ->
                        entry.getValue() != null &&
                                entry.getValue().equals(entry.getKey() + 1))
                .count();
    }

    // Получить попытку по id
    public Attempt getAttemptById(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Attempt not found with id: " + id));
    }

    // Все попытки пользователя
    public List<Attempt> getUserAttempts(Long userId) {
        return attemptRepository.findByUserId(userId);
    }
}
