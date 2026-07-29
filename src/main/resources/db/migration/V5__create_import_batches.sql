CREATE TABLE import_batches(
    id BIGSERIAL PRIMARY KEY  ,
    user_id BIGINT NOT NULL ,
    file_name VARCHAR(255) NOT NULL   ,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_rows INTEGER NOT NULL ,
    success_rows INTEGER NOT NULL ,
    failed_rows INTEGER NOT NULL ,
    status VARCHAR(100) NOT NULL  CHECK(status ='COMPLETED' or status='FAILED'or status='PENDING'),
    error_message TEXT,
    CONSTRAINT fk_import_batches_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);