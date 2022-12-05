CREATE TABLE guild
(
    id BIGINT PRIMARY KEY
);

CREATE TABLE self_role
(
    id         TEXT PRIMARY KEY,
    guild_id   BIGINT REFERENCES guild (id),
    message_id BIGINT   NOT NULL,
    role_ids   BIGINT[] NOT NULL
);
