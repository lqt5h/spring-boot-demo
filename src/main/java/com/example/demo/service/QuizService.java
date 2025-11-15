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

    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }

    public Quiz createQuiz(Quiz quiz) {
        quiz.setIsLocked(false);
        return quizRepository.save(quiz);
    }

    public Quiz updateQuiz(Long id, Quiz updatedQuiz) {
        Quiz quiz = getQuizById(id);
        quiz.setTitle(updatedQuiz.getTitle());
        quiz.setDescription(updatedQuiz.getDescription());
        quiz.setAllowMultipleAttempts(updatedQuiz.isAllowMultipleAttempts());
        if (updatedQuiz.isIsLocked()) {
            quiz.setIsLocked(true);
        }
        return quizRepository.save(quiz);
    }

    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQuizStatistics(Long quizId) {
        Quiz quiz = getQuizById(quizId);
        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);

        Map<String, Object> result = new HashMap<>();

        if (attempts == null || attempts.isEmpty()) {
            result.put("quizId", quizId);
            result.put("quizTitle", quiz.getTitle());
            result.put("totalAttempts", 0L);
            result.put("uniqueUsers", 0L);
            result.put("averageScore", 0.0);
            return result;
        }

        long totalAttempts = attempts.size();
        long uniqueUsers = attempts.stream()
                .map(a -> a.getUser().getId())
                .distinct()
                .count();
        double averageScore = attempts.stream()
                .map(Attempt::getScore)           // score = long
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        result.put("quizId", quizId);
        result.put("quizTitle", quiz.getTitle());
        result.put("totalAttempts", totalAttempts);
        result.put("uniqueUsers", uniqueUsers);
        result.put("averageScore",
                Math.round(averageScore * 100.0) / 100.0);

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopScores(Long quizId, int limit) {
        return attemptRepository.findByQuizId(quizId).stream()
                .sorted((a1, a2) -> Long.compare(a2.getScore(), a1.getScore()))
                .limit(limit)
                .map(attempt -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("attemptId", attempt.getId());
                    map.put("username", attempt.getUser().getUsername());
                    map.put("score", attempt.getScore());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Quiz lockQuizIfHasAttempts(Long quizId) {
        Quiz quiz = getQuizById(quizId);
        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);
        if (attempts != null && !attempts.isEmpty()) {
            quiz.setIsLocked(true);
            quizRepository.save(quiz);
        }
        return quiz;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserProgressReport(Long userId) {
        List<Attempt> userAttempts = attemptRepository.findByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);

        if (userAttempts == null || userAttempts.isEmpty()) {
            result.put("totalAttempts", 0L);
            result.put("averageScore", 0.0);
            return result;
        }

        long totalAttempts = userAttempts.size();

        double overallAverage = userAttempts.stream()
                .map(Attempt::getScore)           // long
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        result.put("totalAttempts", totalAttempts);
        result.put("averageScore",
                Math.round(overallAverage * 100.0) / 100.0);

        return result;
    }

    @Transactional
    public Quiz duplicateQuiz(Long sourceQuizId) {
        Quiz sourceQuiz = getQuizById(sourceQuizId);

        Quiz newQuiz = new Quiz();
        newQuiz.setTitle(sourceQuiz.getTitle() + " (Copy)");
        newQuiz.setDescription(sourceQuiz.getDescription());
        newQuiz.setIsLocked(false);
        newQuiz.setAllowMultipleAttempts(sourceQuiz.isAllowMultipleAttempts());
        Quiz savedQuiz = quizRepository.save(newQuiz);

        List<Question> sourceQuestions = questionRepository.findByQuizId(sourceQuizId);
        for (Question sourceQuestion : sourceQuestions) {
            Question newQuestion = new Question();
            newQuestion.setText(sourceQuestion.getText());
            newQuestion.setQuiz(savedQuiz);
            Question savedQuestion = questionRepository.save(newQuestion);

            List<AnswerOption> sourceOptions =
                    answerOptionRepository.findByQuestionId(sourceQuestion.getId());
            for (AnswerOption sourceOption : sourceOptions) {
                AnswerOption newOption = new AnswerOption();
                newOption.setText(sourceOption.getText());
                newOption.setCorrect(sourceOption.isCorrect());
                newOption.setQuestion(savedQuestion);
                answerOptionRepository.save(newOption);
            }
        }

        return savedQuiz;
    }
}
