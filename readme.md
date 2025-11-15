README.md (обновлённый, полный)
markdown# Демо-проект Spring Boot

Это простой проект на Spring Boot, демонстрирующий использование REST-контроллеров и полноценной системы онлайн-тестирования (quiz).

## Требования

* Java 25
* Maven 3.9.11
* Git
* PostgreSQL (для продакшена) или H2 (для разработки)
* Docker (опционально, для контейнеризации)

## Установка

1. Клонируйте репозиторий:
   git clone https://github.com/lqt5h/spring-boot-demo.git
   cd spring-boot-demo
   text2. Соберите проект:
   mvn clean package
   text3. Настройте базу данных:
- Для PostgreSQL: Создайте БД `quizdb` с пользователем `postgres` / пароль `password`.
- Обновите `src/main/resources/application.properties` если нужно.

4. Запустите:
   mvn spring-boot:run
   textИли с Docker:
   docker-compose up --build
   text## Структура проекта

- **Backend**: REST API для тестов, попыток, пользователей.
- **Frontend**: Thymeleaf-страницы для прохождения тестов.
- **Админка**: Управление тестами (роль ADMIN).
- **Безопасность**: Spring Security с ролями.
- **Экспорт**: PDF результатов.

## API Эндпоинты (Swagger: /swagger-ui.html)

- POST /api/attempts/start?userId=1&quizId=1 — начать попытку.
- POST /api/attempts/{id}/submit — отправить ответы.

## Страницы сайта

| Раздел    | URL             | Описание                            |
| --------- | --------------- | ----------------------------------- |
| Главная   | `/`             | список доступных тестов             |
| Тест      | `/quiz/{id}`    | описание теста, кнопка "начать"     |
| Попытка   | `/attempt/{id}` | прохождение                         |
| Результат | `/results/{id}` | балл и анализ                       |
| Статистика| `/stats`        | список попыток пользователя         |
| Админка   | `/admin/**`     | управление тестами и пользователями |

## Тестирование

- Зарегистрируйтесь: `/register`.
- Войдите: `/login` (admin/admin для админа).
- Создайте тест через API или админку.
- Используйте Postman для API.

## Docker

- `docker-compose up` — запуск app + PostgreSQL.

## Лицензия

MIT