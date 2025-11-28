# Spring Boot Quiz Demo

Это учебный проект на Spring Boot, который реализует REST API для онлайн‑викторины (quiz) с пользователями, попытками прохождения и статистикой.

## Стек технологий

- Java 21+
- Spring Boot
- Spring Web (REST)
- Spring Data JPA (H2 / PostgreSQL)
- Spring Security (Basic Auth, роли USER / ADMIN)
- Maven
- Docker / Docker Compose (опционально)

## Запуск проекта

1. Клонируйте репозиторий:
   git clone https://github.com/lqt5h/spring-boot-demo.git
   cd spring-boot-demo
2. Соберите и запустите приложение:
   mvn clean package
   mvn spring-boot:run
3. По умолчанию приложение доступно по адресу:
   http://localhost:8080
