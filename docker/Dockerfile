FROM postgres:13 AS HoroPostgresDev

ENV POSTGRES_DB horo
ENV POSTGRES_USER horo
ENV POSTGRES_PASSWORD 12345
COPY init.sql /docker-entrypoint-initdb.d/init.sql