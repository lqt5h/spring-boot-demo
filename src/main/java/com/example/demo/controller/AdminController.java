package com.example.demo.controller;

import com.example.demo.service.QuizService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final QuizService quizService;

    public AdminController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/quizzes")
    public String adminQuizzes(Model model) {
        model.addAttribute("quizzes", quizService.getAllQuizzes());
        return "admin/quizzes";
    }

    @GetMapping("/questions")
    public String adminQuestions(Model model) {
        model.addAttribute("quizzes", quizService.getAllQuizzes());
        return "admin/questions";
    }
}