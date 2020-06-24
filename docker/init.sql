CREATE TABLE guilds(
    snowflake BIGINT PRIMARY KEY,
    locale VARCHAR(5) NOT NULL DEFAULT 'en_GB'
);