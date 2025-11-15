package com.example.demo.controller;

import com.example.demo.entity.AnswerOption;
import com.example.demo.entity.Question;
import com.example.demo.service.AnswerOptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answer-options")
public class AnswerOptionController {
    private final AnswerOptionService answerOptionService;

    public AnswerOptionController(AnswerOptionService answerOptionService) {
        this.answerOptionService = answerOptionService;
    }

    @GetMapping("/question/{questionId}")
    public List<AnswerOption> getAnswerOptionsByQuestionId(@PathVariable Long questionId) {
        return answerOptionService.getAnswerOptionsByQuestionId(questionId);
    }

    @PostMapping
    public AnswerOption createAnswerOption(@RequestBody AnswerOption answerOption) {
        Question question = answerOption.getQuestion();
        if (question != null && question.getQuiz() != null && question.getQuiz().isIsLocked()) {
            throw new RuntimeException("Quiz is locked");
        }
        return answerOptionService.createAnswerOption(answerOption);
    }

    @PutMapping("/{id}")
    public AnswerOption updateAnswerOption(@PathVariable Long id, @RequestBody AnswerOption answerOption) {
        AnswerOption existing = answerOptionService.getAnswerOptionsByQuestionId(
                        answerOption.getQuestion().getId()).stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("AnswerOption not found"));
        if (existing.getQuestion().getQuiz().isIsLocked()) {
            throw new RuntimeException("Quiz is locked");
        }
        return answerOptionService.updateAnswerOption(id, answerOption);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnswerOption(@PathVariable Long id) {
        answerOptionService.deleteAnswerOption(id);
        return ResponseEntity.noContent().build();
    }
}