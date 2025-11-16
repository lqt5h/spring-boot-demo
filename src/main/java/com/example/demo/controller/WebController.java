package com.example.demo.controller;

import com.example.demo.entity.Attempt;
import com.example.demo.service.AttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    @Autowired
    private AttemptService attemptService;

    // Старт попытки из web-интерфейса
    @GetMapping("/start")
    public String startAttempt(@RequestParam long userId,
                               @RequestParam Long quizId,
                               Model model) {
        Attempt attempt = attemptService.startAttempt(userId, quizId);
        model.addAttribute("attempt", attempt);
        return "attempt"; // имя View-шаблона
    }

    // Просмотр попытки по id
    @GetMapping("/attempt/{id}")
    public String getAttemptById(@PathVariable Long id, Model model) {
        Attempt attempt = attemptService.getAttemptById(id);
        model.addAttribute("attempt", attempt);
        return "attempt";
    }

    // Отправка ответов из формы
    @PostMapping("/submit")
    public String submitAttempt(@RequestParam Long attemptId,
                                @RequestParam Map<String, String> rawAnswers,
                                Model model) {

        // Преобразуем Map<String, String> -> Map<String, Object>,
        // чтобы тип совпадал с AttemptService.submitAttempt
        Map<String, Object> answers = new HashMap<>();
        for (Map.Entry<String, String> entry : rawAnswers.entrySet()) {
            // можно оставить строкой, можно конвертировать в число
            // если ответы - числа, попробовать парсить:
            String key = entry.getKey();
            String value = entry.getValue();

            Object castValue = value;
            try {
                castValue = Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // если не число, оставляем как строку
            }

            answers.put(key, castValue);
        }

        Attempt attempt = attemptService.submitAttempt(attemptId, answers);
        model.addAttribute("attempt", attempt);
        return "result"; // имя View-шаблона с результатом
    }

    // Список попыток пользователя
    @GetMapping("/user-attempts/{userId}")
    public String getUserAttempts(@PathVariable long userId, Model model) {
        List<Attempt> attempts = attemptService.getUserAttempts(userId);
        model.addAttribute("attempts", attempts);
        return "user-attempts";
    }
}
