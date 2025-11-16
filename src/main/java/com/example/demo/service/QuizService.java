package com.example.demo.service;

import com.example.demo.entity.AnswerOption;
import com.example.demo.entity.Attempt;
import com.example.demo.entity.Question;
import com.example.demo.entity.Quiz;
import com.example.demo.repository.AnswerOptionRepository;
import com.example.demo.repository.AttemptRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final AttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;

    public QuizService(QuizRepository quizRepository,
                       AttemptRepository attemptRepository,
                       QuestionRepository questionRepository,
                       AnswerOptionRepository answerOptionRepository) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
    }

    // ================= БАЗОВЫЕ ОПЕРАЦИИ =================

    // Все викторины
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    // Викторина по id
    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + id));
    }

    // Создать викторину
    public Quiz createQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    // Обновить викторину
    public Quiz updateQuiz(Long id, Quiz updatedQuiz) {
        Quiz existing = getQuizById(id);

        if (existing.isIsLocked()) {
            throw new IllegalStateException("Quiz is locked and cannot be modified");
        }

        existing.setTitle(updatedQuiz.getTitle());
        existing.setDescription(updatedQuiz.getDescription());
        existing.setAllowMultipleAttempts(updatedQuiz.isAllowMultipleAttempts());

        return quizRepository.save(existing);
    }

    // Удалить викторину
    public void deleteQuiz(Long id) {
        if (!quizRepository.existsById(id)) {
            throw new IllegalArgumentException("Quiz not found with id: " + id);
        }
        quizRepository.deleteById(id);
    }

    // ================= СТАТИСТИКА И ТОП РЕЗУЛЬТАТЫ =================

    // Статистика по викторине
    public Map<String, Object> getQuizStatistics(Long quizId) {
        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);

        long totalAttempts = attempts.size();
        long uniqueUsers = attempts.stream()
                .map(a -> a.getUser().getId())
                .distinct()
                .count();

        double averageScore = attempts.stream()
                .mapToLong(Attempt::getScore)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("quizId", quizId);
        stats.put("totalAttempts", totalAttempts);
        stats.put("uniqueUsers", uniqueUsers);
        stats.put("averageScore", averageScore);

        return stats;
    }

    // Топ результатов по викторине
    public List<Map<String, Object>> getTopScores(Long quizId, int limit) {
        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);

        return attempts.stream()
                .sorted((a1, a2) -> Long.compare(a2.getScore(), a1.getScore()))
                .limit(limit)
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("attemptId", a.getId());
                    m.put("userId", a.getUser().getId());
                    m.put("username", a.getUser().getUsername());
                    m.put("score", a.getScore());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ================= ДУБЛИРОВАНИЕ И БЛОКИРОВКА =================

    // Дублировать викторину вместе с вопросами и вариантами
    @Transactional
    public Quiz duplicateQuiz(Long quizId) {
        Quiz original = getQuizById(quizId);

        Quiz copy = new Quiz();
        copy.setTitle(original.getTitle() + " (Copy)");
        copy.setDescription(original.getDescription());
        copy.setAllowMultipleAttempts(original.isAllowMultipleAttempts());
        copy.setIsLocked(false);

        Quiz savedCopy = quizRepository.save(copy);

        List<Question> questions = questionRepository.findByQuizId(original.getId());
        for (Question q : questions) {
            Question newQ = new Question();
            newQ.setQuiz(savedCopy);
            newQ.setText(q.getText());
            Question savedQ = questionRepository.save(newQ);

            List<AnswerOption> options = answerOptionRepository.findByQuestionId(q.getId());
            for (AnswerOption opt : options) {
                AnswerOption newOpt = new AnswerOption();
                newOpt.setQuestion(savedQ);
                newOpt.setText(opt.getText());
                newOpt.setCorrect(opt.isCorrect());
                answerOptionRepository.save(newOpt);
            }
        }

        return savedCopy;
    }

    // Жёсткая блокировка викторины
    @Transactional
    public Quiz lockQuiz(Long quizId) {
        Quiz quiz = getQuizById(quizId);
        quiz.setIsLocked(true);
        return quizRepository.save(quiz);
    }

    // Блокировка, если есть попытки (опционально)
    @Transactional
    public void lockQuizIfHasAttempts(Long quizId) {
        Quiz quiz = getQuizById(quizId);
        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);

        if (!attempts.isEmpty()) {
            quiz.setIsLocked(true);
            quizRepository.save(quiz);
        }
    }

    // ================= ОТЧЁТ ПО ПРОГРЕССУ ПОЛЬЗОВАТЕЛЯ =================

    public Map<String, Object> getUserProgressReport(Long userId) {
        List<Attempt> attempts = attemptRepository.findByUserId(userId);

        long totalAttempts = attempts.size();
        long quizzesTried = attempts.stream()
                .map(Attempt::getQuizId)
                .distinct()
                .count();

        double averageScore = attempts.stream()
                .mapToLong(Attempt::getScore)
                .average()
                .orElse(0.0);

        long maxScore = attempts.stream()
                .mapToLong(Attempt::getScore)
                .max()
                .orElse(0L);

        Map<String, Object> report = new HashMap<>();
        report.put("userId", userId);
        report.put("totalAttempts", totalAttempts);
        report.put("quizzesTried", quizzesTried);
        report.put("averageScore", averageScore);
        report.put("maxScore", maxScore);

        return report;
    }
}
