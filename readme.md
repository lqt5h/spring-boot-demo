# Quiz API (Spring Boot + TLS)

Учебный REST‑сервис викторины на Spring Boot с поддержкой HTTPS/TLS, JWT‑аутентификации и CI/CD на GitHub Actions.

## Возможности

- Регистрация и аутентификация пользователей по JWT.
- Работа только по HTTPS с самоподписанной цепочкой сертификатов (Root CA → Intermediate CA → Server).
- Эндпоинты для работы с пользователями и викториной (пример: `/hello`, `/api/users/{id}`, `/h2-console`).
- Конфигурация через переменные окружения и `.env` — паролей и ключей нет в репозитории.
- Автоматическая сборка и публикация JAR‑артефакта в GitHub Actions.

## Технологии

- Java 21, Spring Boot 3
- Spring Web, Spring Security, Spring Data JPA
- PostgreSQL (основная БД), H2 in‑memory (тестовый профиль)
- Maven
- GitHub Actions (CI)

## Конфигурация окружения

Все чувствительные данные задаются через переменные окружения или файл `.env` в корне проекта. Файл `.env` добавлен в `.gitignore` и не коммитится в репозиторий.

В `application.properties` используются только ссылки на переменные


Тестовый профиль (`src/test/resources/application-test.properties`) использует H2 in‑memory и отключённый TLS.

## Локальный запуск

1. Установить Java 21 и Maven.
2. Создать файл `.env` по примеру выше.
3. Убедиться, что в `/etc/hosts` есть запись:


Тестовый профиль (`src/test/resources/application-test.properties`) использует H2 in‑memory и отключённый TLS.

## Локальный запуск

1. Установить Java 21 и Maven.
2. Создать файл `.env` по примеру выше.
3. Убедиться, что в `/etc/hosts` есть запись:
   127.0.0.1 quiz-api.local
4. Запустить PostgreSQL и создать БД `quizdb` (или скорректировать `DB_URL` в `.env`).
5. Собрать и запустить приложение:
   mvn clean test
   mvn spring-boot:run

6. Открыть в браузере:
- `https://quiz-api.local:8080` — стартовая страница,
- `https://quiz-api.local:8080/hello` — тестовый endpoint,
- `https://quiz-api.local:8080/h2-console` — консоль H2 (для тестового профиля).

## TLS и сертификаты

Цепочка сертификатов генерируется локально с помощью OpenSSL и содержит идентификатор студента:

- `root_ca_1BIB23392.crt` — корневой сертификат (Root CA).
- `intermediate_ca_1BIB23392.crt` — промежуточный центр сертификации (Intermediate CA).
- `server_1BIB23392.crt` — серверный сертификат для домена `quiz-api.local`.

Root CA устанавливается в доверенные корневые центры ОС/браузера, что позволяет открывать `https://quiz-api.local:8080` без предупреждений о недоверенном сертификате.

## CI/CD (GitHub Actions)

Workflow `.github/workflows/ci.yml` выполняет:

- восстановление TLS keystore из секретов репозитория:
- `KEYSTORE_BASE64` — содержимое PKCS#12 в Base64;
- `KEYSTORE_PASSWORD` — пароль keystore;
- экспорт секретов окружения:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
- `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`;
- запуск тестов: `mvn clean test`;
- сборку артефакта: `mvn clean package -DskipTests`;
- загрузку JAR‑файла `quiz-api-jar` в раздел Artifacts.

Все пароли, токены и ключи хранятся только в `.env` (локально) и GitHub Secrets (в CI), что предотвращает утечку чувствительных данных через репозиторий.



