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

    // GET /api/quizzes - все викторины
    @GetMapping
    public List<Quiz> getAllQuizzes() {
        return quizService.getAllQuizzes();
    }

    // GET /api/quizzes/{id} - викторина по id
    @GetMapping("/{id}")
    public Quiz getQuizById(@PathVariable Long id) {
        return quizService.getQuizById(id);
    }

    // POST /api/quizzes - создать викторину
    @PostMapping
    public Quiz createQuiz(@RequestBody Quiz quiz) {
        return quizService.createQuiz(quiz);
    }

    // PUT /api/quizzes/{id} - обновить викторину
    @PutMapping("/{id}")
    public Quiz updateQuiz(@PathVariable Long id, @RequestBody Quiz quiz) {
        return quizService.updateQuiz(id, quiz);
    }

    // DELETE /api/quizzes/{id} - удалить викторину
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/quizzes/{id}/statistics - статистика
    @GetMapping("/{id}/statistics")
    public Map<String, Object> getQuizStatistics(@PathVariable Long id) {
        return quizService.getQuizStatistics(id);
    }

    // GET /api/quizzes/{id}/top-scores - топ результаты
    @GetMapping("/{id}/top-scores")
    public List<Map<String, Object>> getTopScores(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "10") int limit) {
        return quizService.getTopScores(id, limit);
    }

    // POST /api/quizzes/{id}/lock - заблокировать викторину
    @PostMapping("/{id}/lock")
    public ResponseEntity<Quiz> lockQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.lockQuiz(id));
    }

    // POST /api/quizzes/{id}/duplicate - дублировать викторину
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Quiz> duplicateQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.duplicateQuiz(id));
    }
}
