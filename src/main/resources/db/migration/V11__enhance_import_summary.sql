ALTER TABLE import_batches
    ADD COLUMN skipped_rows INTEGER NOT NULL DEFAULT 0;

ALTER TABLE import_batch_errors
    ADD COLUMN description VARCHAR(255),
    ADD COLUMN category_name VARCHAR(100);
