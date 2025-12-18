# Quiz API (Spring Boot)

REST API для управления квизами (викторинами): квизы, вопросы, варианты ответов, попытки прохождения и роли USER/TEACHER/ADMIN. [file:109]  
Проект поддерживает **два** способа аутентификации для защищённых эндпоинтов: Bearer JWT и HTTP Basic. [file:109]

## Возможности

- CRUD для квизов, вопросов и вариантов ответов. [file:109]
- Прохождение квиза (attempts) и получение результатов/аналитики (см. Postman бизнес-операции). [file:103]
- Auth endpoints (`/auth/*`) выдают access/refresh токены, а refresh-токены ведутся как сессии в БД и ротируются при refresh. [file:107]

## Запуск

### Локально (Maven)
mvn clean test
mvn spring-boot:run

Базовый URL (по умолчанию): `http://localhost:8080` или `https://localhost:8080` — зависит от настроек SSL в `application.properties`. [file:110]

## Аутентификация

### Публичные endpoints
Без аутентификации доступны:
- `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh` [file:109]
- `GET /hello` [file:109]
- `GET /h2-console/**` [file:109]

### Пара токенов (login/refresh)
Ответ DTO содержит: `accessToken`, `refreshToken`, `tokenType` (обычно `Bearer`), `accessTokenExpiresIn`. [file:106]

#### Login
curl -k -X POST "https://localhost:8080/auth/login"
-H "Content-Type: application/json"
-d '{"username":"teacher","password":"Passwd0rd1!"}

#### Refresh (refresh-rotation)
Refresh-логика:
- refresh JWT валидируется,
- сессия ищется в БД по refreshToken,
- если активна и не истекла — старая помечается `REVOKED`, создаётся новая сессия и новая пара токенов. [file:107]

curl -k -X POST "https://localhost:8080/auth/refresh"
-H "Content-Type: application/json"
-d '{"refreshToken":"<REFRESH_TOKEN>"}'

### Доступ к защищённым endpoints

#### Вариант A: Bearer JWT
Так как в `SecurityConfig` включён `oauth2ResourceServer().jwt(...)`, защищённые эндпоинты могут принимать `Authorization: Bearer <accessToken>`. [file:109]

Пример:
curl -k "https://localhost:8080/api/quizzes"
-H "Authorization: Bearer <ACCESS_TOKEN>"

#### Вариант B: HTTP Basic
Так как в `SecurityConfig` также включён `httpBasic()`, можно обращаться и через basic-аутентификацию. [file:109]

Пример:
curl -k -u teacher:Passwd0rd1! "https://localhost:8080/api/quizzes"

## Роли и доступ

Точные ограничения по ролям/методам задаются в `SecurityConfig` (например, часть операций доступна только ADMIN или TEACHER). [file:109]  
Если тестируешь через Postman — удобнее сначала получить токены через `/auth/login`, затем использовать `Bearer` (или включить basic auth для конкретных запросов). [file:109]

## Админ: refresh-сессии

### GET /admin/sessions
Контроллер `AdminSessionController` возвращает список refresh-сессий из БД. [file:108]  
В ответе есть поля: `id`, `userId`, `refreshToken`, `status`, `createdAt`, `expiresAt`, `revokedAt`. [file:108]

Пример (Bearer):
curl -k "https://localhost:8080/admin/sessions"
-H "Authorization: Bearer <ACCESS_TOKEN>"


Пример (Basic):
curl -k -u admin:Passwd0rd1! "https://localhost:8080/admin/sessions"

> Внимание: endpoint возвращает `refreshToken` в явном виде — это чувствительные данные; для продакшена обычно токен не отдают или маскируют/хэшируют. [file:108]

## Тестирование

### Postman
- `Quiz-API-Tests.postman_collection.json` — набор тестов API. [file:103]
- `Quiz-API-Business-Operations-Tests.postman_collection.json` — бизнес-сценарии (создание полного квиза → редактирование → публикация → прохождение → аналитика). [file:103]

Импортируй коллекции в Postman и настрой переменные окружения (baseUrl, токены/учётные данные). [file:103]

## Примечания по безопасности

- Refresh-токены хранятся как `UserSession` в БД и имеют статусы `ACTIVE/REVOKED/EXPIRED`. [file:107]
- При refresh используется ротация: старый refresh становится `REVOKED`, создаётся новый refresh и новая access-пара. [file:107]
- Для отзыва всех сессий пользователя предусмотрена логика `revokeAllSessions(...)`. [file:107]
