Create table accounts(
    id BIGSERIAL primary key ,
    user_id BIGINT NOT NULL ,
    provider_id BIGINT NOT NULL ,
    account_name VARCHAR(100) NOT NULL ,
    account_number VARCHAR(50) UNIQUE  NOT NULL   ,
    account_type varchar(20) NOT NULL ,
    balance DECIMAL(19,2) NOT  NULL CHECK (balance >=0),
    is_active BOOLEAN DEFAULT  TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_account_provider FOREIGN KEY (provider_id) REFERENCES providers(id)   ON DELETE RESTRICT

);