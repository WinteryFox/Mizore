CREATE TABLE guilds
(
    snowflake BIGINT PRIMARY KEY,
    locale    VARCHAR(5) DEFAULT null
);

CREATE TABLE prefixes
(
    snowflake BIGINT,
    prefix    VARCHAR(5),
    PRIMARY KEY (snowflake, prefix)
);

CREATE TABLE users
(
    snowflake BIGINT PRIMARY KEY,
    locale VARCHAR(5) NOT NULL DEFAULT 'en-GB'
);