package com.example.storyai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Spring Boot can actually connect to the MySQL development database
 * (TASK-003 acceptance: "Spring Boot 能成功连接数据库").
 *
 * <p>Only runs when {@code DB_PASSWORD} is set, so the default {@code mvn test}
 * stays database-independent. Run with:
 * <pre>
 *   DB_USERNAME=story_dev DB_PASSWORD=storypass mvn test
 * </pre>
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void canConnectToMysql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(3), "MySQL connection should be valid within 3s");
        }
    }
}
