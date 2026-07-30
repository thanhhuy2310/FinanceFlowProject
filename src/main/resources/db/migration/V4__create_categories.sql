CREATE  table  categories
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT  NOT NULL  ,
    name       VARCHAR(100) NOT NULL  ,
    type       VARCHAR(20)  NOT NULL check (type = 'INCOME' or type = 'EXPENSE'),
    icon       VARCHAR(100),
    color      VARCHAR(20),
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT  unq_userid_name unique (user_id,name),
    CONSTRAINT fk_categories_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);