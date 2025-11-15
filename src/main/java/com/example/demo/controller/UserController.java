package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.QuizService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuizService quizService;

    @PostMapping("/create")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<Map<String, Object>> getUserProgress(@PathVariable Long id) {
        // Бросит RuntimeException, если пользователя нет
        userService.getUserById(id);
        // Безопасный отчёт о прогрессе пользователя
        Map<String, Object> report = quizService.getUserProgressReport(id);
        return ResponseEntity.ok(report);
    }
}
