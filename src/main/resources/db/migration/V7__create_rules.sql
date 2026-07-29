CREATE TABLE rules(
    id BIGSERIAL PRIMARY KEY ,
    user_id BIGINT NOT NULL unique ,
    category_id BIGINT NOT NULL ,
    keyword VARCHAR(100) NOT NULL unique ,
    priority INT NOT NULL DEFAULT 1 CHECK ( priority >1 ),
    is_active BOOLEAN DEFAULT TRUE NOT NULL ,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rules_users FOREIGN KEY  (user_id) REFERENCES  users(id) ON DELETE RESTRICT ,
    CONSTRAINT fk_rules_categories FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);