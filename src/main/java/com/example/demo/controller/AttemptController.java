package com.example.demo.controller;

import com.example.demo.entity.Attempt;
import com.example.demo.service.AttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/attempts")
public class AttemptController {

    @Autowired
    private AttemptService attemptService;

    @PostMapping("/start")
    public Attempt startAttempt(@RequestParam Long userId, @RequestParam Long quizId) {
        return attemptService.startAttempt(userId, quizId);
    }

    @PostMapping("/submit")
    public Attempt submitAttempt(@RequestParam Long attemptId, @RequestBody Map<Long, Long> answers) {
        return attemptService.submitAttempt(attemptId, answers);
    }

    @GetMapping("/{id}")
    public Attempt getAttemptById(@PathVariable Long id) {
        return attemptService.getAttemptById(id);
    }

    @GetMapping("/user/{userId}")
    public List<Attempt> getUserAttempts(@PathVariable Long userId) {
        return attemptService.getUserAttempts(userId);
    }
}
