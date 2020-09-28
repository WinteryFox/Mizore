CREATE TABLE guilds
(
    snowflake BIGINT PRIMARY KEY,
    locale    VARCHAR(5) DEFAULT null
);

CREATE TABLE prefixes
(
    snowflake BIGINT REFERENCES guilds (snowflake),
    prefix    VARCHAR(5),
    PRIMARY KEY (snowflake, prefix)
);

CREATE TABLE users
(
    snowflake BIGINT PRIMARY KEY,
    locale    VARCHAR(5) NOT NULL DEFAULT 'en-GB'
);

CREATE TABLE animals
(
    snowflake  BIGINT REFERENCES users (snowflake),
    id         SERIAL      NOT NULL,
    type       SMALLINT    NOT NULL,
    name       VARCHAR(16) NOT NULL,
    level      SMALLINT    NOT NULL DEFAULT 1,
    experience INT         NOT NULL DEFAULT 0,
    hunger     SMALLINT    NOT NULL DEFAULT 100,
    boredom    SMALLINT    NOT NULL DEFAULT 100,
    PRIMARY KEY (snowflake, id)
);