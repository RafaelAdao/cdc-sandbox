# WIP
- [x] Starting Zookeeper
- [x] Starting Kafka
- [x] Starting a Postgres database
  - [x] Setup database schema and initial data
- [x] Starting Kafka Connect
- [x] Deploying the Postgres connector
  - [x] Registering a connector to monitor the cdc_sandbox_experiment_1 database
- [x] Watch data changes
  - [x] Updating the database and viewing the update event
- [x] Cleaning up
- [ ] Move all parts to a docker-compose
- [ ] Elasticsearch
- [ ] Apache Beam

# Walk through with Postgres

## Starting Zookeeper

```sh
docker run -it --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 quay.io/debezium/zookeeper:2.1
```

## Starting Kafka

```sh
docker run -it --rm --name kafka -p 9092:9092 --link zookeeper:zookeeper quay.io/debezium/kafka:2.1
```

## Starting a Postgres database

```sh
docker run -it --rm --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_USER=postgres -v ./postgresql.conf:/etc/postgresql/postgresql.conf postgres -c config_file=/etc/postgresql/postgresql.conf
```

### Setup database schema and initial data

```sh
docker exec -it postgres psql -U postgres

CREATE DATABASE cdc_sandbox_experiment_1;
\c cdc_sandbox_experiment_1;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE my_table (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  description VARCHAR(255),
  value FLOAT,
  timestamp TIMESTAMP
);

INSERT INTO my_table (description, value, timestamp)
VALUES
  ('Product A', 10.50, NOW()),
  ('Product B', 20.75, NOW()),
  ('Product C', 15.00, NOW()),
  ('Product D', 8.99, NOW()),
  ('Product E', 12.25, NOW());
```

## Starting Kafka Connect

```sh
docker run -it --rm --name connect -p 8083:8083 -e GROUP_ID=1 -e CONFIG_STORAGE_TOPIC=my_connect_configs -e OFFSET_STORAGE_TOPIC=my_connect_offsets -e STATUS_STORAGE_TOPIC=my_connect_statuses --link kafka:kafka --link postgres:postgres quay.io/debezium/connect:2.1

curl -H "Accept:application/json" localhost:8083/
curl -H "Accept:application/json" localhost:8083/connectors/
```

## Deploying the Postgres connector

### Registering a connector to monitor the cdc_sandbox_experiment_1 database

```sh
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{
  "name": "experiment-1-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname" : "cdc_sandbox_experiment_1",
    "topic.prefix": "xp-1",
    "schema.include.list": "public",
    "plugin.name": "pgoutput"
  }
}'

curl -H "Accept:application/json" localhost:8083/connectors/
curl -i -X GET -H "Accept:application/json" localhost:8083/connectors/experiment-1-connector
# curl -i -X DELETE -H "Accept:application/json" localhost:8083/connectors/experiment-1-connector
```

## Watch data changes

```sh
docker run -it --rm --name watcher --link zookeeper:zookeeper --link kafka:kafka quay.io/debezium/kafka:2.1 watch-topic -a -k xp-1.public.my_table
```

### Updating the database and viewing the update event

```sh
UPDATE my_table SET description='Anne Marie';
```

## Cleaning up

```sh
docker stop postgres watcher connect kafka zookeeper
```
