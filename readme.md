# Quiz API (Spring Boot)

REST API для управления квизами (викторинами): квизы, вопросы, варианты ответов, попытки прохождения и роли **USER / TEACHER / ADMIN**.[1][2][3][4]
Проект поддерживает два способа доступа к защищённым эндпоинтам: **Bearer JWT** и **HTTP Basic**.[5]

***

## Возможности

- Полный CRUD для квизов, вопросов и вариантов ответов.[2][3][1]
- Прохождение квиза (attempts), пересчёт результатов и расширенная аналитика.[6][7][1]
- Auth‑эндпоинты (`/auth/*`) выдают пару токенов (access/refresh) и реализуют refresh‑rotation с хранением refresh‑сессий в БД.[5]
- Административные операции: управление пользователями и refresh‑сессиями.[4]

***

## Стек и запуск

### Технологический стек

- Java + Spring Boot (REST‑контроллеры, валидация, глобальный обработчик ошибок).[8][9]
- JWT‑аутентификация + `oauth2ResourceServer().jwt(...)` и параллельно `httpBasic()`.[5]
- Реляционная БД (конфигурация через `application.properties` / `application.yml`), для refresh‑сессий используется сущность `UserSession`.

### Локальный запуск (Maven)

```bash
mvn clean test
mvn spring-boot:run
```

Базовый URL по умолчанию: `http://localhost:8080` или `https://localhost:8080` — в зависимости от настроек SSL в конфигурации.[10]

Для быстрой проверки можно вызвать health‑endpoint `GET /hello`.[11]

***

## Аутентификация и роли

### Публичные эндпоинты

Без аутентификации доступны:[11][5]

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /hello`
- `GET /h2-console/**` (для in‑memory БД при разработке)

### JWT‑пара (login / refresh)

DTO ответа при логине/обновлении содержит: `accessToken`, `refreshToken`, `tokenType` (обычно `Bearer`), `accessTokenExpiresIn`.

- **Login** — `POST /auth/login` с `username` и `password`, возвращает пару токенов и данные пользователя.[5]
- **Refresh** — `POST /auth/refresh` с телом `{ "refreshToken": "<REFRESH_TOKEN>" }`.[5]

Логика refresh‑rotation:
- refresh‑JWT валидируется;
- в БД ищется активная `UserSession` по refreshToken;
- при успехе старая сессия помечается `REVOKED`, создаётся новая сессия и новая пара токенов;
- истёкший или уже отозванный refresh даёт ошибку авторизации.

Refresh‑токены хранятся с полями `id`, `userId`, `refreshToken`, `status (ACTIVE/REVOKED/EXPIRED)`, `createdAt`, `expiresAt`, `revokedAt`; есть служебная логика `revokeAllSessions(...)` для массового отзыва.

### Доступ к защищённым эндпоинтам

**Вариант A — Bearer JWT**  
Защищённые эндпоинты принимают заголовок:[5]

```http
Authorization: Bearer <ACCESS_TOKEN>
```

**Вариант B — HTTP Basic**  
Альтернативно можно использовать basic‑аутентификацию:

```http
Authorization: Basic base64(username:password)
```

В тестах/клиентах обычно:
- для пользовательских и бизнес‑операций — Bearer JWT после `/auth/login`;
- для отладочных / административных вызовов можно задействовать Basic.

### Роли

Реализованы роли:[4][5]

- **USER** — проходит опубликованные квизы, создаёт/отправляет свои попытки, просматривает историю попыток.[6][4]
- **TEACHER** — создаёт и редактирует квизы, вопросы и варианты, публикует квизы и получает аналитику, может корректировать попытки.[7][1][6]
- **ADMIN** — управляет пользователями и refresh‑сессиями, имеет расширенные права доступа.

Точные правила доступа по ролям описаны в `SecurityConfig` (ант‑матчи под конкретные URL).

***

## Основные сущности домена

- **User** — id, username, email, role; пароль не возвращается в API‑ответах.[4]
- **Quiz** — id, title, description, allowMultipleAttempts, isLocked и др.[1]
- **Question** — id, text, ссылка на quiz.[2]
- **AnswerOption** — id, text, флаг `isCorrect`, ссылка на question.[3]
- **Attempt** — id, user, quiz, score, details, finishedAt, список ответов по вопросам.[6]
- **UserSession** — сущность refresh‑сессии (управление JWT‑refresh).

***

## Контроллеры и эндпоинты

### AuthController (`/auth`)[5]

- `POST /auth/register` — регистрация нового пользователя с ролью `USER` или `TEACHER`.
- `POST /auth/login` — выдача пары токенов и пользовательских данных.
- `POST /auth/refresh` — обновление пары токенов по действующему refresh‑JWT.

### UserController (`/api/users`)[4]

- `GET /api/users/{id}` — получить пользователя по id (без пароля).
- `POST /api/users/create` — административное создание пользователя с нужной ролью (например, TEACHER).

### QuizController (`/api/quizzes`)[1]

- `GET /api/quizzes` — список всех квизов.
- `GET /api/quizzes/{quizId}` — квиз по id; при отсутствии возвращается понятная ошибка.
- `POST /api/quizzes` — создание квиза с полями `title`, `description`, `allowMultipleAttempts`.
- `PUT /api/quizzes/{quizId}` — обновление квиза; попытка изменить заблокированный квиз приводит к бизнес‑ошибке.
- `DELETE /api/quizzes/{quizId}` — удаление квиза и связанных данных.
- `POST /api/quizzes/{quizId}/lock` — установить `isLocked = true`, запретив изменения структуры.
- `POST /api/quizzes/{quizId}/duplicate` — создать копию квиза (новый id, изменённое название, без блокировки).
- `GET /api/quizzes/{quizId}/statistics` — агрегированная статистика по квизу.
- `GET /api/quizzes/{quizId}/top-scores` — список лучших результатов (лимит задаётся query‑параметром).

### QuestionController (`/api/questions`)[2]

- `GET /api/questions/quiz/{quizId}` — список вопросов по квизу.
- `POST /api/questions` — создание вопроса; для locked‑квиза операция запрещена.
- `PUT /api/questions/{questionId}` — обновление текста/ссылок вопроса.
- `DELETE /api/questions/{questionId}` — удаление вопроса и связанных вариантов / ответов в попытках.

### AnswerOptionController (`/api/answer-options`)[3]

- `GET /api/answer-options/question/{questionId}` — получить варианты ответа по вопросу.
- `POST /api/answer-options` — создание варианта; запрещено для заблокированного квиза.
- `PUT /api/answer-options/{optionId}` — обновление текста/флага корректности.
- `DELETE /api/answer-options/{optionId}` — удаление варианта.

Контроллеры вопросов и вариантов также используются при валидации ответов в попытках (проверка, что выбранный вариант принадлежит вопросу).[3][6]

### AttemptController (`/api/attempts`)[6]

- `POST /api/attempts/start` — создать новую попытку для пользователя и квиза, с проверкой существования и бизнес‑ограничений.
- `PUT /api/attempts/{attemptId}/answers` — обновить ответы в незавершённой попытке; принимает массив `{ questionId, selectedOptionId }` с дополнительной валидацией и проверкой прав.
- `POST /api/attempts/{attemptId}/submit` — завершить попытку: выставить `finishedAt`, пересчитать `score`, зафиксировать правильность ответов.
- `GET /api/attempts/{attemptId}` — получить полную информацию о попытке.
- `GET /api/attempts/user/{userId}` — список попыток пользователя.
- `PUT /api/attempts/{attemptId}` — ручное обновление попытки (TEACHER/ADMIN могут скорректировать `score`, `details`, `finishedAt`).

### TeacherController (`/api/teacher/quiz`) — бизнес‑операции[7]

Высокоуровневые операции для преподавателя, объединяющие несколько базовых шагов.

- `POST /api/teacher/quiz/create-full` — создать **полный квиз** в одном запросе:
    - создаётся квиз;
    - добавляются вопросы;
    - для каждого вопроса создаются варианты с указанием `correctOptionIndex`.
- `PUT /api/teacher/quiz/{quizId}/edit` — изменить `title`, `description`, `allowMultipleAttempts` до начала реальных попыток.
- `POST /api/teacher/quiz/{quizId}/publish` — опубликовать квиз и заблокировать структуру (questions/options).
- `GET /api/teacher/quiz/{quizId}/analytics` — расширенная аналитика по квизу (totalAttempts, uniqueUsers, averageScore и др.).

Эти операции формируют сквозные сценарии «создать полный квиз → отредактировать до первых попыток → опубликовать и заблокировать → дать пользователям пройти → получить аналитику», работая поверх базовых CRUD‑эндпоинтов.[7][1][2][3][6]

### Админ‑часть

- **AdminController (`/api/admin`)** — административные действия (например, управление пользователями); стиль похож на UserController, доступ только для ADMIN.[4]
- **AdminSessionController (`/admin/sessions`)** —  
  `GET /admin/sessions` возвращает список refresh‑сессий (`id`, `userId`, `refreshToken`, `status`, `createdAt`, `expiresAt`, `revokedAt`).   
  Этот эндпоинт удобен для проверки refresh‑rotation и управления активными сессиями, но в продакшене возвращать `refreshToken` в явном виде не рекомендуется.

### Веб‑ и вспомогательные контроллеры

- **HelloController** — простой health‑check (`GET /hello`) для проверки `base_url`.[11]
- **MainController / WebController** — отдают веб‑страницы и базовую навигацию при наличии UI.[9]

***

## Обработка ошибок

Глобальный обработчик `GlobalExceptionHandler` централизует формирование ответов об ошибках.[8]

- Обрабатывает типовые исключения (`IllegalArgumentException`, `IllegalStateException` и др.) и возвращает JSON с полями `error`, `message`, `timestamp`, `status`, `path`.[8]
- Конвертирует бизнес‑ошибки («Quiz locked», «Attempt not found», «Not your attempt», «answers is required» и др.) в соответствующие HTTP‑коды (`400`, `403`, `404`) с человекочитаемыми сообщениями, которые используют все контроллеры.[1][6]

Это даёт единый формат ошибок для клиентов и упрощает написание автотестов.[8][11]

***

## Тестирование и сценарные кейсы

Сервис поддерживает полноценные сценарии от регистрации пользователей до аналитики по квизам; они строятся на комбинации контроллеров `AuthController`, `TeacherController`, `AttemptController`, `QuizController`, `QuestionController`, `AnswerOptionController`, а также административных контроллеров.[2][3][7][1][6][5]

### Бизнес‑операции (one‑shot сценарии)

Отдельно выделены **бизнес‑операции преподавателя**, реализованные в `TeacherController` и связанных контроллерах:[7][1][6]

- Создание **полного квиза** одним запросом с вопросами и вариантами (`create-full`).[7]
- Редактирование параметров квиза до появления попыток (`edit`).[7]
- Публикация и блокировка структуры квиза (после чего менять вопросы/варианты нельзя) (`publish`).[3][1][2][7]
- Прохождение квиза пользователем с формированием попытки, ответов и итогового `score`.[6]
- Получение **расширенной аналитики** по квизу: количество попыток, уникальных пользователей, средний балл и др. (`analytics`).[1][7]

Эти операции формируют сквозные сценарии «создать → настроить → опубликовать → пройти → проанализировать», поверх базовых CRUD‑эндпоинтов.[2][3][1][6][7]

