package com.example.demo.controller;

import com.example.demo.entity.AnswerOption;
import com.example.demo.entity.Attempt;
import com.example.demo.entity.AttemptAnswer;
import com.example.demo.entity.User;
import com.example.demo.repository.AnswerOptionRepository;
import com.example.demo.repository.AttemptAnswerRepository;
import com.example.demo.repository.AttemptRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AnswerOptionRepository answerOptionRepository;

    public AttemptController(
            AttemptRepository attemptRepository,
            UserRepository userRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            AnswerOptionRepository answerOptionRepository
    ) {
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.answerOptionRepository = answerOptionRepository;
    }

    // ===== DTOs =====

    public static class StartAttemptRequest {
        private Long userId;
        private Long quizId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }
    }

    public static class UpdateAttemptRequest {
        private String details;
        private Long score;
        private String finishedAt; // ISO: 2025-12-17T15:30:00

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public Long getScore() { return score; }
        public void setScore(Long score) { this.score = score; }

        public String getFinishedAt() { return finishedAt; }
        public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }
    }

    public static class UpdateAttemptAnswersRequest {
        private List<AnswerItem> answers;

        public List<AnswerItem> getAnswers() { return answers; }
        public void setAnswers(List<AnswerItem> answers) { this.answers = answers; }
    }

    public static class AnswerItem {
        private Long questionId;
        private Long selectedOptionId;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }

        public Long getSelectedOptionId() { return selectedOptionId; }
        public void setSelectedOptionId(Long selectedOptionId) { this.selectedOptionId = selectedOptionId; }
    }

    // ===== helpers =====

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private User currentUserOr401() {
        Authentication a = auth();
        if (a == null || a.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String username = a.getName();

        // предполагается, что в UserRepository есть findByUsername(String)
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private boolean isTeacherOrAdmin(User u) {
        if (u == null || u.getRole() == null) return false;
        String role = u.getRole().trim().toUpperCase();
        return "TEACHER".equals(role) || "ADMIN".equals(role);
    }

    private void requireTeacherOrAdmin() {
        User u = currentUserOr401();
        if (!isTeacherOrAdmin(u)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TEACHER or ADMIN only");
        }
    }

    private Attempt getAttemptOr404(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found: " + id));
    }

    private void requireAttemptOwnerOrTeacherOrAdmin(Attempt attempt) {
        User me = currentUserOr401();

        // TEACHER/ADMIN может править любые попытки
        if (isTeacherOrAdmin(me)) return;

        // иначе только владелец
        if (attempt.getUser() == null || attempt.getUser().getUsername() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Attempt has no owner");
        }

        if (!attempt.getUser().getUsername().equals(me.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your attempt");
        }
    }

    private long recalcScore(Long attemptId) {
        return attemptAnswerRepository.findByAttemptId(attemptId).stream()
                .filter(AttemptAnswer::isCorrect)
                .count();
    }

    // ===== endpoints =====

    // POST /api/attempts/start - начать попытку
    @PostMapping("/start")
    public Attempt startAttempt(@RequestBody StartAttemptRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + request.getUserId()
                ));

        Attempt attempt = new Attempt();
        attempt.setUser(user);
        attempt.setQuizId(request.getQuizId());
        attempt.setDetails(null);
        attempt.setScore(0L);
        attempt.setFinishedAt(null);

        return attemptRepository.save(attempt);
    }

    // PUT /api/attempts/{id}/answers - редактировать ответы
    // USER может только свою и пока не finished; TEACHER/ADMIN может любую (в т.ч. после finished)
    @PutMapping("/{id}/answers")
    public Attempt updateAttemptAnswers(@PathVariable Long id, @RequestBody UpdateAttemptAnswersRequest request) {
        Attempt attempt = getAttemptOr404(id);
        requireAttemptOwnerOrTeacherOrAdmin(attempt);

        User me = currentUserOr401();
        boolean teacherOrAdmin = isTeacherOrAdmin(me);

        // ограничение "пока не закончена попытка" только для USER
        if (!teacherOrAdmin && attempt.getFinishedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attempt already finished");
        }

        if (request == null || request.getAnswers() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answers is required");
        }

        for (AnswerItem item : request.getAnswers()) {
            if (item == null || item.getQuestionId() == null || item.getSelectedOptionId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each answer must contain questionId and selectedOptionId"
                );
            }

            AnswerOption option = answerOptionRepository.findById(item.getSelectedOptionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "AnswerOption not found: " + item.getSelectedOptionId()
                    ));

            if (option.getQuestion() == null || option.getQuestion().getId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AnswerOption has no question");
            }

            if (!option.getQuestion().getId().equals(item.getQuestionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "selectedOptionId does not belong to questionId");
            }

            AttemptAnswer attemptAnswer = attemptAnswerRepository
                    .findByAttemptIdAndQuestionId(id, item.getQuestionId())
                    .orElseGet(AttemptAnswer::new);

            attemptAnswer.setAttempt(attempt);
            attemptAnswer.setQuestion(option.getQuestion());
            attemptAnswer.setSelectedOption(option);
            attemptAnswer.setCorrect(option.isCorrect());

            attemptAnswerRepository.save(attemptAnswer);
        }

        long score = recalcScore(id);
        attempt.setScore(score);
        attempt.setDetails("Answers updated");

        return attemptRepository.save(attempt);
    }

    // POST /api/attempts/{id}/submit - завершить попытку
    @PostMapping("/{id}/submit")
    public Attempt submitAttempt(@PathVariable Long id) {
        Attempt attempt = getAttemptOr404(id);
        requireAttemptOwnerOrTeacherOrAdmin(attempt);

        User me = currentUserOr401();
        boolean teacherOrAdmin = isTeacherOrAdmin(me);

        if (!teacherOrAdmin && attempt.getFinishedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attempt already finished");
        }

        long score = recalcScore(id);
        attempt.setScore(score);
        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setDetails("Submitted with score: " + score);

        return attemptRepository.save(attempt);
    }

    // GET /api/attempts/{id} - получить попытку
    @GetMapping("/{id}")
    public Attempt getAttemptById(@PathVariable Long id) {
        return getAttemptOr404(id);
    }

    // GET /api/attempts/user/{userId} - все попытки пользователя
    @GetMapping("/user/{userId}")
    public List<Attempt> getUserAttempts(@PathVariable Long userId) {
        return attemptRepository.findByUserId(userId);
    }

    // PUT /api/attempts/{id} - изменить попытку (ТОЛЬКО TEACHER/ADMIN)
    @PutMapping("/{id}")
    public Attempt updateAttempt(@PathVariable Long id, @RequestBody UpdateAttemptRequest request) {
        requireTeacherOrAdmin();

        Attempt attempt = getAttemptOr404(id);

        if (request != null) {
            if (request.getDetails() != null) attempt.setDetails(request.getDetails());
            if (request.getScore() != null) attempt.setScore(request.getScore());
            if (request.getFinishedAt() != null) attempt.setFinishedAt(LocalDateTime.parse(request.getFinishedAt()));
        }

        return attemptRepository.save(attempt);
    }
}
