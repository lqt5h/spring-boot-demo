package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping("/")
    public String home() {
        return """
                ✅ Приложение запущено успешно!<br>
                Доступные маршруты:<br>
                • <a href='/hello'>/hello</a> — приветствие<br>
                • <a href='/api/users/1'>/api/users/{id}</a> — получить пользователя<br>
                • <a href='/h2-console'>/h2-console</a> — консоль базы данных H2<br>
                """;
    }
}
