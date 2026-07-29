CREATE TABLE transactions(
    id BIGSERIAL PRIMARY KEY ,
    account_id BIGINT NOT NULL ,
    category_id BIGINT NOT NULL ,
    import_batch_id BIGINT ,
    amount DECIMAL(19,2) NOT NULL CHECK(amount >=0),
    transaction_type VARCHAR(20) NOT NULL CHECK(transaction_type in('INCOME','EXPENSE')),
    transaction_date TIMESTAMP NOT NULL  DEFAULT  CURRENT_TIMESTAMP,
    description VARCHAR(255),
    reference VARCHAR(100),
    attachment_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT  fk_transactions_users FOREIGN KEY (account_id) references accounts(id) ON DELETE RESTRICT ,
    CONSTRAINT  fk_transactions_categories FOREIGN KEY (category_id) references categories(id) ON DELETE RESTRICT,
    CONSTRAINT  fk_transactions_import FOREIGN KEY (import_batch_id) references import_batches(id) ON DELETE SET NULL
);