-- =========================================================================
-- Flyway Migration: Fix Category Uniqueness Constraint
-- Description: Drop legacy global UNIQUE(name) on categories and ensure
--              composite UNIQUE(user_id, name) constraint is enforced.
-- =========================================================================

DO $$
DECLARE
    r RECORD;
BEGIN
    -- 1. Drop any single-column unique constraints on categories(name)
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

    -- 2. Drop any legacy unique index on categories(name) if present
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
        END;
    END IF;
END $$;
