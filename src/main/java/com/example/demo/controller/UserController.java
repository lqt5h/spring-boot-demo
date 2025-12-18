package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.QuizManagementService;
import com.example.demo.service.QuizService;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final QuizService quizService;
    private final QuizManagementService quizManagementService;

    public UserController(UserService userService, QuizService quizService, QuizManagementService quizManagementService) {
        this.userService = userService;
        this.quizService = quizService;
        this.quizManagementService = quizManagementService;
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username);
    }

    private void requireUserRole() {
        User u = currentUser();
        if (!"USER".equalsIgnoreCase(u.getRole())) {
            throw new RuntimeException("Access denied. Only USER role can perform this action");
        }
    }

    // ================== Старые endpoints (сохранены) ==================

    // Создать пользователя (если нужно) — как и было, но учти: SecurityConfig ограничивает это ADMIN-ом
    @PostMapping("/create")
    public User createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        created.setPassword(null);
        return created;
    }

    // Получить пользователя по id (пароль скрываем)
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        user.setPassword(null);
        return user;
    }

    // Отчёт по прогрессу пользователя (старый endpoint)
    // ВАЖНО: чтобы USER не смотрел прогресс другого USER — разрешим:
    // - самому себе
    // - TEACHER / ADMIN
    @GetMapping("/{id}/progress")
    public ResponseEntity<?> getUserProgress(@PathVariable Long id) {
        User caller = currentUser();

        boolean isOwner = caller.getId() != null && caller.getId().equals(id);
        boolean isTeacherOrAdmin = "TEACHER".equalsIgnoreCase(caller.getRole()) || "ADMIN".equalsIgnoreCase(caller.getRole());

        if (!isOwner && !isTeacherOrAdmin) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "You can only view your own progress");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

        // выбросит исключение, если пользователя нет
        User user = userService.getUserById(id);
        user.setPassword(null);

        Map<String, Object> report = quizService.getUserProgressReport(id);
        return ResponseEntity.ok(report);
    }

    // ================== Новые endpoints "как StudentController" ==================

    // USER видит только опубликованные (locked=true) квизы
    @GetMapping("/quizzes/available")
    public ResponseEntity<?> getAvailableQuizzes() {
        requireUserRole();

        var all = quizService.getAllQuizzes();
        var available = all.stream().filter(q -> q.isIsLocked()).toList();

        Map<String, Object> out = new HashMap<>();
        out.put("total", available.size());
        out.put("quizzes", available);
        return ResponseEntity.ok(out);
    }

    // БИЗ-ОП #4: one-shot submit квиза (ответы + завершение)
    // ВАЖНО: score сейчас = количество правильных (как в AttemptController.recalcScore)
    @PostMapping("/quiz/{quizId}/submit")
    public ResponseEntity<?> submitQuiz(@PathVariable Long quizId,
                                        @RequestBody QuizManagementService.SubmitQuizRequest req) {
        requireUserRole();

        User me = currentUser();
        Map<String, Object> result = quizManagementService.submitQuizAttempt(me.getId(), quizId, req == null ? null : req.answers);
        return ResponseEntity.ok(result);
    }

    // Удобный endpoint: прогресс текущего пользователя (без id)
    @GetMapping("/me/progress")
    public ResponseEntity<?> myProgress() {
        requireUserRole();
        User me = currentUser();
        return ResponseEntity.ok(quizService.getUserProgressReport(me.getId()));
    }

    // Явно запрещенные операции (как у тебя было в прошлой логике)
    @PostMapping("/quiz/create")
    public ResponseEntity<Map<String, String>> forbiddenCreateQuiz() {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Forbidden");
        error.put("message", "Students cannot create quizzes. Contact your teacher.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @PutMapping("/quiz/{id}/edit")
    public ResponseEntity<Map<String, String>> forbiddenEditQuiz(@PathVariable Long id) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Forbidden");
        error.put("message", "Students cannot edit quizzes.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @GetMapping("/quiz/{quizId}/all-results")
    public ResponseEntity<Map<String, String>> forbiddenViewAllResults(@PathVariable Long quizId) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Forbidden");
        error.put("message", "Students can only see their own results.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
