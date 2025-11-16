package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.QuizService;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final QuizService quizService;

    public UserController(UserService userService, QuizService quizService) {
        this.userService = userService;
        this.quizService = quizService;
    }

    // Создать пользователя (если нужно)
    @PostMapping("/create")
    public User createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        created.setPassword(null);
        return created;
    }

    // Получить пользователя по id
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        // чтобы пароль не возвращался в ответе
        user.setPassword(null);
        return user;
    }

    // Отчёт по прогрессу пользователя
    @GetMapping("/{id}/progress")
    public ResponseEntity<Map<String, Object>> getUserProgress(@PathVariable Long id) {
        // выбросит исключение, если пользователя нет
        User user = userService.getUserById(id);
        user.setPassword(null);

        Map<String, Object> report = quizService.getUserProgressReport(id);
        return ResponseEntity.ok(report);
    }
}
