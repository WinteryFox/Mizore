CREATE TABLE guild
(
    id BIGINT PRIMARY KEY
);

CREATE TABLE self_role
(
    id          TEXT PRIMARY KEY CHECK (length(id) > 0 AND length(id) < 256),
    guild_id    BIGINT REFERENCES guild (id),
    message_id  BIGINT   NOT NULL,
    role_ids    BIGINT[] NOT NULL,
    description TEXT CHECK (description IS NULL OR (length(description) > 0 AND length(description) < 4096)),
    image_url   TEXT,
    color       INT
);
