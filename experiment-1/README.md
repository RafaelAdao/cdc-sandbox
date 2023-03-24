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

## Deploying the Postgres connector

### Registering a connector to monitor the cdc_sandbox_experiment_1 database

```sh
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d @register-postgres.json
```

#### Check the connector status

```sh
curl -H "Accept:application/json" localhost:8083/connectors/
curl -i -X GET -H "Accept:application/json" localhost:8083/connectors/experiment-1-connector/status
# curl -i -X DELETE -H "Accept:application/json" localhost:8083/connectors/experiment-1-connector
```

## Cleaning up

```sh
docker-compose down
```
