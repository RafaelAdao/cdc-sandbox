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
- [x] Move all parts to a docker-compose
- [ ] Apache Beam
- [ ] Elasticsearch

# Walk through with Postgres

## Starting

```sh
docker-compose up -d
```

### Check the xp-1.public.my_table topic was created

```sh
docker exec kafka ./bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```

## Deploying the Postgres connector to monitor the cdc_sandbox_experiment_1 database

```sh
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d @register-postgres.json
```

### Check the connector status

```sh
curl -H "Accept:application/json" localhost:8083/connectors/
curl -i -X GET -H "Accept:application/json" localhost:8083/connectors/experiment-1-connector/status
# curl -i -X DELETE -H "Accept:application/json" localhost:8083/connectors/experiment-1-connector
```

## Update the database and see the changes on the topic

- watcher startup. This will print all the messages on the topic from the beginning

```sh
docker exec kafka ./bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic xp-1.public.my_table --from-beginning
```

- update the database

```sh
docker exec -it postgres psql -U postgres -d cdc_sandbox_experiment_1
```

```sql
UPDATE my_table SET value = value * 2;
```

## Cleaning up

```sh
docker-compose down
```
