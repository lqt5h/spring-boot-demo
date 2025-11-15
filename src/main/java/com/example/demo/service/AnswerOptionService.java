package com.example.demo.service;

import com.example.demo.entity.AnswerOption;
import com.example.demo.repository.AnswerOptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnswerOptionService {
    private final AnswerOptionRepository answerOptionRepository;

    public AnswerOptionService(AnswerOptionRepository answerOptionRepository) {
        this.answerOptionRepository = answerOptionRepository;
    }

    public List<AnswerOption> getAnswerOptionsByQuestionId(Long questionId) {
        return answerOptionRepository.findByQuestionId(questionId);
    }

    public AnswerOption createAnswerOption(AnswerOption answerOption) {
        return answerOptionRepository.save(answerOption);
    }

    public AnswerOption updateAnswerOption(Long id, AnswerOption updatedOption) {
        AnswerOption existing = answerOptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AnswerOption not found"));
        existing.setText(updatedOption.getText());
        existing.setCorrect(updatedOption.isCorrect()); // Исправлено с isIsCorrect на isCorrect
        return answerOptionRepository.save(existing);
    }

    public void deleteAnswerOption(Long id) {
        answerOptionRepository.deleteById(id);
    }
}