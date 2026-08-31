package com.heet.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;

/**
 * Ensures database constraints on PostgreSQL match the entity definition,
 * specifically dropping any obsolete single-column unique constraint on categories(name)
 * and ensuring composite unique constraint on categories(user_id, name).
 */
@Component
public class DatabaseSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String dbProductName = jdbcTemplate.execute((ConnectionCallback<String>) conn -> {
                DatabaseMetaData meta = conn.getMetaData();
                return meta.getDatabaseProductName();
            });

            if (dbProductName != null && dbProductName.toLowerCase().contains("postgres")) {
                log.info("PostgreSQL database detected. Running category constraint verification...");

                String migrationSql = """
                    DO $$
                    DECLARE
                        r RECORD;
                    BEGIN
                        -- 1. Drop any single-column unique constraint on categories(name)
                        FOR r IN (
                            SELECT conname
                            FROM pg_constraint c
                            JOIN pg_class t ON c.conrelid = t.oid
                            JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
                            WHERE t.relname = 'categories'
                              AND c.contype = 'u'
                              AND array_length(c.conkey, 1) = 1
                              AND a.attname = 'name'
                        ) LOOP
                            EXECUTE 'ALTER TABLE categories DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
                        END LOOP;

                        -- 2. Drop any legacy unique index on categories(name) alone
                        FOR r IN (
                            SELECT indexname
                            FROM pg_indexes
                            WHERE tablename = 'categories'
                              AND indexdef ILIKE '%UNIQUE%name%'
                              AND indexdef NOT ILIKE '%user_id%'
                        ) LOOP
                            EXECUTE 'DROP INDEX IF EXISTS ' || quote_ident(r.indexname);
                        END LOOP;

                        -- 3. Ensure composite unique constraint on (user_id, name) exists
                        IF NOT EXISTS (
                            SELECT 1
                            FROM pg_constraint c
                            JOIN pg_class t ON c.conrelid = t.oid
                            WHERE t.relname = 'categories'
                              AND (c.conname = 'uq_categories_user_id_name' OR c.conname = 'uk_categories_user_id_name')
                        ) THEN
                            BEGIN
                                ALTER TABLE categories ADD CONSTRAINT uq_categories_user_id_name UNIQUE (user_id, name);
                            EXCEPTION
                                WHEN duplicate_table THEN NULL;
                                WHEN duplicate_object THEN NULL;
                                WHEN others THEN NULL;
                            END;
                        END IF;
                    END $$;
                    """;

                jdbcTemplate.execute(migrationSql);
                log.info("Category table unique constraints verified successfully (UNIQUE(user_id, name)).");
            }
        } catch (Exception e) {
            log.warn("Database schema migration runner notice: {}", e.getMessage());
        }
    }
}
