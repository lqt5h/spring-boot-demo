-- Инициализация тестовых данных

INSERT INTO users (id, username, email, role, enabled, password) VALUES
                                                                     (1, 'student1', 'student1@example.com', 'USER', true, '$2a$10$ZIvxYWu.Ov7e0KhpBdAp..kFRKiY0Z1h9jC.x3VvSi8yJ0LZ3PvGK'),
                                                                     (2, 'student2', 'student2@example.com', 'USER', true, '$2a$10$ZIvxYWu.Ov7e0KhpBdAp..kFRKiY0Z1h9jC.x3VvSi8yJ0LZ3PvGK'),
                                                                     (3, 'teacher1', 'teacher1@example.com', 'TEACHER', true, '$2a$10$ZIvxYWu.Ov7e0KhpBdAp..kFRKiY0Z1h9jC.x3VvSi8yJ0LZ3PvGK'),
                                                                     (4, 'admin1', 'admin1@example.com', 'ADMIN', true, '$2a$10$ZIvxYWu.Ov7e0KhpBdAp..kFRKiY0Z1h9jC.x3VvSi8yJ0LZ3PvGK')
    ON CONFLICT DO NOTHING;

INSERT INTO quiz (id, title, description, is_locked, allow_multiple_attempts, created_at) VALUES
                                                                                              (1, 'JavaScript Basics', 'Basic questions about JavaScript', false, true, now()),
                                                                                              (2, 'Java Spring Framework', 'Advanced Spring Boot questions', false, false, now()),
                                                                                              (3, 'SQL Fundamentals', 'SQL query basics', false, true, now())
    ON CONFLICT DO NOTHING;

INSERT INTO question (id, quiz_id, text, created_at) VALUES
                                                         (1, 1, 'What is the correct way to declare a variable in JavaScript?', now()),
                                                         (2, 1, 'What is the output of console.log(typeof undefined)?', now()),
                                                         (3, 1, 'Which method converts a string to a number?', now())
    ON CONFLICT DO NOTHING;

INSERT INTO answer_option (id, question_id, text, is_correct) VALUES
                                                                  (1, 1, 'let x = 5;', true),
                                                                  (2, 1, 'variable x = 5;', false),
                                                                  (3, 1, 'x := 5;', false),
                                                                  (4, 1, 'x = 5;', false),
                                                                  (5, 2, 'undefined', true),
                                                                  (6, 2, 'null', false),
                                                                  (7, 2, 'string', false),
                                                                  (8, 2, 'object', false),
                                                                  (9, 3, 'Number() or parseInt()', true),
                                                                  (10, 3, 'String()', false),
                                                                  (11, 3, 'Boolean()', false),
                                                                  (12, 3, 'toNumber()', false)
    ON CONFLICT DO NOTHING;

INSERT INTO attempt (id, user_id, quiz_id, score, started_at, finished_at, created_at) VALUES
                                                                                           (1, 1, 1, 2, now(), now(), now()),
                                                                                           (2, 1, 1, 3, now(), now(), now()),
                                                                                           (3, 2, 1, 2, now(), now(), now())
    ON CONFLICT DO NOTHING;
