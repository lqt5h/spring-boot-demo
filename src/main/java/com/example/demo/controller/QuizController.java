package com.example.demo.controller;

import com.example.demo.entity.Quiz;
import com.example.demo.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public List<Quiz> getAllQuizzes() {
        return quizService.getAllQuizzes();
    }

    @GetMapping("/{id}")
    public Quiz getQuizById(@PathVariable Long id) {
        return quizService.getQuizById(id);
    }

    @PostMapping
    public Quiz createQuiz(@RequestBody Quiz quiz) {
        return quizService.createQuiz(quiz);
    }

    @PutMapping("/{id}")
    public Quiz updateQuiz(@PathVariable Long id, @RequestBody Quiz quiz) {
        if (quizService.getQuizById(id).isIsLocked()) {
            throw new RuntimeException("Quiz is locked and cannot be modified");
        }
        return quizService.updateQuiz(id, quiz);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getQuizStatistics(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizStatistics(id));
    }

    @GetMapping("/{id}/top-scores")
    public ResponseEntity<List<Map<String, Object>>> getTopScores(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(quizService.getTopScores(id, limit));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<Quiz> lockQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.lockQuizIfHasAttempts(id));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Quiz> duplicateQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.duplicateQuiz(id));
    }
}
