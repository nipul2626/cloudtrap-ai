
package com.cloudtrap.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
class PostgreSqlPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToPostgreSqlAndFindFlywayMigration() {
        String database = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        );

        Integer successfulMigrations = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1'
                  AND success = TRUE
                """,
                Integer.class
        );

        assertThat(database).isEqualTo("cloudtrap");
        assertThat(successfulMigrations).isNotNull();
        assertThat(successfulMigrations).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldContainAllIdentityAndWorkspaceTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name IN (
                      'users',
                      'workspaces',
                      'workspace_memberships',
                      'cloud_accounts'
                  )
                """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(4);
    }
}
