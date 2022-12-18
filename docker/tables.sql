CREATE TABLE guild
(
    id BIGINT PRIMARY KEY
);

CREATE TABLE self_role
(
    id          UUID PRIMARY KEY,
    guild_id    BIGINT REFERENCES guild (id),
    role_ids    BIGINT[] NOT NULL,
    channel_id  BIGINT   NOT NULL,
    message_id  BIGINT   NOT NULL,
    title       TEXT     NOT NULL CHECK (length(title) > 0 AND length(title) < 256),
    "label"     TEXT     NOT NULL CHECK (length(self_role.label) > 0 AND length(self_role.label) < 80),
    description TEXT CHECK (description IS NULL OR (length(description) > 0 AND length(description) < 4096)),
    image_url   TEXT,
    color       INT
);
