CREATE TABLE import_batch_errors (
    id BIGSERIAL PRIMARY KEY,
    import_batch_id BIGINT NOT NULL,
    row_number INTEGER NOT NULL,
    error_message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_batch_errors_batch
        FOREIGN KEY (import_batch_id) REFERENCES import_batches(id) ON DELETE CASCADE
);

CREATE INDEX idx_import_batch_errors_batch_id ON import_batch_errors(import_batch_id);
