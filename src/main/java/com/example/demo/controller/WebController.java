package com.example.demo.controller;

import com.example.demo.entity.Attempt;
import com.example.demo.service.AttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@Controller
public class WebController {

    @Autowired
    private AttemptService attemptService;

    @GetMapping("/start")
    public String startAttempt(@RequestParam long userId, @RequestParam Long quizId, Model model) {
        Attempt attempt = attemptService.startAttempt(userId, quizId);
        model.addAttribute("attempt", attempt);
        return "attempt";
    }

    @GetMapping("/attempt/{id}")
    public String getAttemptById(@PathVariable Long id, Model model) {
        Attempt attempt = attemptService.getAttemptById(id);
        model.addAttribute("attempt", attempt);
        return "attempt";
    }

    @PostMapping("/submit")
    public String submitAttempt(@RequestParam Long attemptId, @RequestBody Map<Long, Long> answers, Model model) {
        Attempt attempt = attemptService.submitAttempt(attemptId, answers);
        model.addAttribute("attempt", attempt);
        return "result";
    }

    @GetMapping("/user-attempts/{userId}")
    public String getUserAttempts(@PathVariable long userId, Model model) {
        List<Attempt> attempts = attemptService.getUserAttempts(userId);
        model.addAttribute("attempts", attempts);
        return "user-attempts";
    }
}