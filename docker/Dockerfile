FROM postgres:15.1 AS mizore-api-postgres

ENV POSTGRES_PASSWORD 12345

COPY tables.sql /docker-entrypoint-initdb.d/tables.sql
