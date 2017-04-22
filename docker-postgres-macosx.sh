#!/usr/bin/env bash
cp ./read-backend/src/main/resources/db/migrations/V1_0__ReadTables.sql ./docker-postgres/V1_0__ReadTables.sql
cp ./write-backend/src/main/resources/db/migrations/V1_0__Journals.sql ./docker-postgres/V1_0__Journals.sql
docker build -t ihavemoney/postgres ./docker-postgres && \

docker run --name ihavemoney-postgres -v /Users/Shared/postgres:/var/lib/postgresql \
 -d -p 5432:5432 -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=changeit ihavemoney/postgres
