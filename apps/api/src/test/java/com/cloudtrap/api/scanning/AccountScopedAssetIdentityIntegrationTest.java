
package com.cloudtrap.api.scanning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
class AccountScopedAssetIdentityIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveAccountScopedUniqueIndexForCloudAssets() {
        Map<String, Object> index = findIndex(
                "uq_assets_account_identity"
        );

        assertThat(index).isNotEmpty();

        String definition = index.get("indexdef").toString()
                .toLowerCase();

        assertThat(definition)
                .contains("unique index")
                .contains("workspace_id")
                .contains("cloud_account_id")
                .contains("provider")
                .contains("external_id")
                .contains("where (cloud_account_id is not null)");
    }

    @Test
    void shouldHaveSeparateUniqueIndexForUnscopedAssets() {
        Map<String, Object> index = findIndex(
                "uq_assets_unscoped_identity"
        );

        assertThat(index).isNotEmpty();

        String definition = index.get("indexdef").toString()
                .toLowerCase();

        assertThat(definition)
                .contains("unique index")
                .contains("workspace_id")
                .contains("provider")
                .contains("external_id")
                .contains("where (cloud_account_id is null)");
    }

    @Test
    void shouldNotRetainTheOldAssetIdentityConstraint() {
        Integer oldConstraintCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conrelid = 'public.assets'::regclass
                  AND conname = 'uq_assets_identity'
                """,
                Integer.class
        );

        assertThat(oldConstraintCount).isZero();
    }

    private Map<String, Object> findIndex(String indexName) {
        return jdbcTemplate.queryForMap(
                """
                SELECT indexname, indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'assets'
                  AND indexname = ?
                """,
                indexName
        );
    }
}
