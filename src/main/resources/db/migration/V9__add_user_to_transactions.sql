ALTER TABLE transactions
    ADD COLUMN user_id BIGINT;

UPDATE transactions transaction_record
SET user_id = account.user_id
FROM accounts account
WHERE transaction_record.account_id = account.id;

ALTER TABLE transactions
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

CREATE INDEX idx_transactions_user_id ON transactions (user_id);
