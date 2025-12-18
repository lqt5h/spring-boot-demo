package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizManagementService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    public QuizManagementService(
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            AnswerOptionRepository answerOptionRepository,
            AttemptRepository attemptRepository,
            AttemptAnswerRepository attemptAnswerRepository
    ) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
    }

    // ========= DTOs =========

    public static class CreateFullQuizRequest {
        public String title;
        public String description;
        public Boolean allowMultipleAttempts;
        public List<CreateQuestionRequest> questions;
    }

    public static class CreateQuestionRequest {
        public String text;
        public List<String> options;
        public Integer correctOptionIndex;
    }

    public static class SubmitQuizRequest {
        // key = questionId, value = selectedOptionId
        public Map<Long, Long> answers;
    }

    // ========= БИЗ-ОП #1: создать квиз целиком =========
    @Transactional
    public Quiz createFullQuiz(CreateFullQuizRequest req) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        if (req.title == null || req.title.isBlank()) throw new IllegalArgumentException("title is required");
        if (req.questions == null || req.questions.isEmpty()) throw new IllegalArgumentException("questions must not be empty");

        Quiz quiz = new Quiz();
        quiz.setTitle(req.title);
        quiz.setDescription(req.description);
        quiz.setAllowMultipleAttempts(req.allowMultipleAttempts == null ? true : req.allowMultipleAttempts);
        quiz.setIsLocked(false);

        List<Question> questionEntities = new ArrayList<>();

        for (CreateQuestionRequest qReq : req.questions) {
            if (qReq == null || qReq.text == null || qReq.text.isBlank()) {
                throw new IllegalArgumentException("Each question must have text");
            }
            if (qReq.options == null || qReq.options.size() < 2) {
                throw new IllegalArgumentException("Each question must have at least 2 options");
            }
            if (qReq.correctOptionIndex == null || qReq.correctOptionIndex < 0 || qReq.correctOptionIndex >= qReq.options.size()) {
                throw new IllegalArgumentException("correctOptionIndex is invalid");
            }

            Question question = new Question();
            question.setText(qReq.text);
            question.setQuiz(quiz);

            List<AnswerOption> optionEntities = new ArrayList<>();
            for (int i = 0; i < qReq.options.size(); i++) {
                String optText = qReq.options.get(i);
                if (optText == null || optText.isBlank()) {
                    throw new IllegalArgumentException("Answer option text must not be empty");
                }
                AnswerOption opt = new AnswerOption();
                opt.setText(optText);
                opt.setCorrect(i == qReq.correctOptionIndex);
                opt.setQuestion(question);
                optionEntities.add(opt);
            }

            question.setAnswerOptions(optionEntities);
            questionEntities.add(question);
        }

        quiz.setQuestions(questionEntities);

        // за счет cascade на Quiz.questions и Question.answerOptions сохранится дерево целиком
        return quizRepository.save(quiz);
    }

    // ========= БИЗ-ОП #2: редактировать квиз до попыток =========
    @Transactional
    public Quiz editQuizBeforeAttempts(Long quizId, String newTitle, String newDescription, Boolean allowMultipleAttempts) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        if (quiz.isIsLocked()) {
            throw new IllegalStateException("Quiz is locked and cannot be modified");
        }

        if (!attemptRepository.findByQuizId(quizId).isEmpty()) {
            throw new IllegalStateException("Cannot edit quiz with existing attempts");
        }

        if (newTitle != null && !newTitle.isBlank()) quiz.setTitle(newTitle);
        if (newDescription != null) quiz.setDescription(newDescription);
        if (allowMultipleAttempts != null) quiz.setAllowMultipleAttempts(allowMultipleAttempts);

        return quizRepository.save(quiz);
    }

    // ========= БИЗ-ОП #3: публикация/блокировка =========
    @Transactional
    public Map<String, Object> publishAndLock(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        List<Question> questions = questionRepository.findByQuizId(quizId);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Cannot publish quiz without questions");
        }

        for (Question q : questions) {
            List<AnswerOption> opts = answerOptionRepository.findByQuestionId(q.getId());
            if (opts.size() < 2) {
                throw new IllegalStateException("Each question must have at least 2 options");
            }
            if (opts.stream().noneMatch(AnswerOption::isCorrect)) {
                throw new IllegalStateException("Each question must have at least one correct option");
            }
        }

        quiz.setIsLocked(true);
        quizRepository.save(quiz);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("quizId", quiz.getId());
        out.put("status", "PUBLISHED");
        out.put("isLocked", true);
        out.put("questionsCount", questions.size());
        out.put("message", "Quiz published (locked=true).");
        return out;
    }

    // ========= БИЗ-ОП #4: submit квиза одним запросом =========
    @Transactional
    public Map<String, Object> submitQuizAttempt(Long userId, Long quizId, Map<Long, Long> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("answers is required");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        if (!quiz.isIsLocked()) {
            throw new IllegalStateException("Quiz is not published yet (locked=false)");
        }

        // проверка allowMultipleAttempts
        if (!quiz.isAllowMultipleAttempts()) {
            boolean already = attemptRepository.findByUserId(userId).stream()
                    .anyMatch(a -> Objects.equals(a.getQuizId(), quizId));
            if (already) {
                throw new IllegalStateException("Multiple attempts are not allowed for this quiz");
            }
        }

        Attempt attempt = new Attempt();
        User u = new User();
        u.setId(userId); // важно: достаточно id, JPA привяжет по FK (user_id)
        attempt.setUser(u);
        attempt.setQuizId(quizId);
        attempt.setScore(0);
        attempt.setDetails("Started and submitted in one request");
        attempt.setFinishedAt(null);

        Attempt savedAttempt = attemptRepository.save(attempt);

        long correct = 0;

        // проверим, что вопрос действительно из этого квиза
        Set<Long> quizQuestionIds = questionRepository.findByQuizId(quizId).stream()
                .map(Question::getId)
                .collect(Collectors.toSet());

        for (Map.Entry<Long, Long> e : answers.entrySet()) {
            Long questionId = e.getKey();
            Long optionId = e.getValue();

            if (!quizQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException("Question " + questionId + " does not belong to quiz " + quizId);
            }

            AnswerOption option = answerOptionRepository.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("AnswerOption not found: " + optionId));

            if (option.getQuestion() == null || option.getQuestion().getId() == null) {
                throw new IllegalArgumentException("AnswerOption has no question: " + optionId);
            }
            if (!option.getQuestion().getId().equals(questionId)) {
                throw new IllegalArgumentException("selectedOptionId does not belong to questionId");
            }

            boolean isCorrect = option.isCorrect();
            if (isCorrect) correct++;

            AttemptAnswer aa = new AttemptAnswer();
            aa.setAttempt(savedAttempt);
            aa.setQuestion(option.getQuestion());
            aa.setSelectedOption(option);
            aa.setCorrect(isCorrect);
            attemptAnswerRepository.save(aa);
        }

        savedAttempt.setScore(correct);
        savedAttempt.setFinishedAt(LocalDateTime.now());
        savedAttempt.setDetails("Submitted with correct answers: " + correct);
        attemptRepository.save(savedAttempt);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", savedAttempt.getId());
        out.put("score", savedAttempt.getScore()); // у тебя score сейчас = кол-во верных
        out.put("correctAnswers", correct);
        out.put("finishedAt", savedAttempt.getFinishedAt());
        return out;
    }

    // ========= БИЗ-ОП #5: аналитика =========
    @Transactional(readOnly = true)
    public Map<String, Object> analytics(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);
        List<Question> questions = questionRepository.findByQuizId(quizId);

        long totalAttempts = attempts.size();
        long uniqueUsers = attempts.stream()
                .filter(a -> a.getUser() != null)
                .map(a -> a.getUser().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        double averageScore = attempts.stream()
                .mapToLong(Attempt::getScore)
                .average()
                .orElse(0.0);

        // соберем все ответы всех попыток (без findByQuestionId — его нет)
        List<AttemptAnswer> allAnswers = attempts.stream()
                .flatMap(a -> attemptAnswerRepository.findByAttemptId(a.getId()).stream())
                .collect(Collectors.toList());

        Map<String, Object> questionStatistics = new LinkedHashMap<>();
        for (Question q : questions) {
            List<AnswerOption> opts = answerOptionRepository.findByQuestionId(q.getId());

            Map<Long, Long> distribution = new LinkedHashMap<>();
            for (AnswerOption opt : opts) {
                long cnt = allAnswers.stream()
                        .filter(aa -> aa.getQuestion() != null && aa.getQuestion().getId().equals(q.getId()))
                        .filter(aa -> aa.getSelectedOption() != null && aa.getSelectedOption().getId().equals(opt.getId()))
                        .count();
                distribution.put(opt.getId(), cnt);
            }

            Map<String, Object> qStat = new LinkedHashMap<>();
            qStat.put("text", q.getText());
            qStat.put("answerDistribution", distribution);
            questionStatistics.put("question_" + q.getId(), qStat);
        }

        List<Map<String, Object>> topScores = attempts.stream()
                .sorted((a1, a2) -> Long.compare(a2.getScore(), a1.getScore()))
                .limit(10)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("attemptId", a.getId());
                    m.put("userId", a.getUser() != null ? a.getUser().getId() : null);
                    m.put("username", a.getUser() != null ? a.getUser().getUsername() : null);
                    m.put("score", a.getScore());
                    m.put("finishedAt", a.getFinishedAt());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("quizId", quiz.getId());
        out.put("quizTitle", quiz.getTitle());
        out.put("totalAttempts", totalAttempts);
        out.put("uniqueUsers", uniqueUsers);
        out.put("averageScore", averageScore);
        out.put("totalQuestions", questions.size());
        out.put("questionStatistics", questionStatistics);
        out.put("topScores", topScores);
        out.put("generatedAt", LocalDateTime.now());
        return out;
    }
}
