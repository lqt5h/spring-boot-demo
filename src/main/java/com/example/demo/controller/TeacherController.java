package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.Quiz;
import com.example.demo.service.QuizManagementService;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final QuizManagementService quizManagementService;
    private final UserService userService;

    public TeacherController(QuizManagementService quizManagementService, UserService userService) {
        this.quizManagementService = quizManagementService;
        this.userService = userService;
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username);
    }

    private void requireTeacherOrAdmin() {
        User u = currentUser();
        if (!"TEACHER".equalsIgnoreCase(u.getRole()) && !"ADMIN".equalsIgnoreCase(u.getRole())) {
            throw new IllegalStateException("Access denied. TEACHER or ADMIN only");
        }
    }

    // БИЗ-ОП #1
    @PostMapping("/quiz/create-full")
    public ResponseEntity<?> createFullQuiz(@RequestBody QuizManagementService.CreateFullQuizRequest req) {
        requireTeacherOrAdmin();
        Quiz created = quizManagementService.createFullQuiz(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // БИЗ-ОП #2 (минимально: правим свойства квиза, только если нет попыток)
    @PutMapping("/quiz/{quizId}/edit")
    public ResponseEntity<?> editQuiz(@PathVariable Long quizId, @RequestBody Map<String, Object> body) {
        requireTeacherOrAdmin();
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        Boolean allowMultipleAttempts = body.get("allowMultipleAttempts") == null ? null : (Boolean) body.get("allowMultipleAttempts");

        Quiz updated = quizManagementService.editQuizBeforeAttempts(quizId, title, description, allowMultipleAttempts);
        return ResponseEntity.ok(updated);
    }

    // БИЗ-ОП #3
    @PostMapping("/quiz/{quizId}/publish")
    public ResponseEntity<?> publish(@PathVariable Long quizId) {
        requireTeacherOrAdmin();
        return ResponseEntity.ok(quizManagementService.publishAndLock(quizId));
    }

    // БИЗ-ОП #5
    @GetMapping("/quiz/{quizId}/analytics")
    public ResponseEntity<?> analytics(@PathVariable Long quizId) {
        requireTeacherOrAdmin();
        return ResponseEntity.ok(quizManagementService.analytics(quizId));
    }
}
