package com.example.demo.controller;

import com.example.demo.entity.Attempt;
import com.example.demo.service.AttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    @Autowired
    private AttemptService attemptService;

    // DTO для старта попытки
    public static class StartAttemptRequest {
        private Long userId;
        private Long quizId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getQuizId() {
            return quizId;
        }

        public void setQuizId(Long quizId) {
            this.quizId = quizId;
        }
    }

    // POST /api/attempts/start - начать попытку
    @PostMapping("/start")
    public Attempt startAttempt(@RequestBody StartAttemptRequest request) {
        return attemptService.startAttempt(request.getUserId(), request.getQuizId());
    }

    // POST /api/attempts/{id}/submit - завершить попытку
    // Тип Map<String, Object> совпадает с AttemptService
    @PostMapping("/{id}/submit")
    public Attempt submitAttempt(@PathVariable Long id,
                                 @RequestBody Map<String, Object> answers) {
        return attemptService.submitAttempt(id, answers);
    }

    // GET /api/attempts/{id} - получить попытку
    @GetMapping("/{id}")
    public Attempt getAttemptById(@PathVariable Long id) {
        return attemptService.getAttemptById(id);
    }

    // GET /api/attempts/user/{userId} - все попытки пользователя
    @GetMapping("/user/{userId}")
    public List<Attempt> getUserAttempts(@PathVariable Long userId) {
        return attemptService.getUserAttempts(userId);
    }
}
