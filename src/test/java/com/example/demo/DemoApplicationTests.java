package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        // Отключаем SSL в тестах
        "server.ssl.enabled=false",

        // Встроенная H2 вместо PostgreSQL
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",

        // JPA настройки
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",

        // ВАЖНО: не выполнять data.sql в тестах
        "spring.sql.init.mode=never"
})
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
