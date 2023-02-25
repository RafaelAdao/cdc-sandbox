# Walk through

[Link](https://debezium.io/documentation/reference/stable/tutorial.html)

## Starting Zookeeper

```sh
docker run -it --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 quay.io/debezium/zookeeper:2.1
```

## Starting Kafka

```sh
docker run -it --rm --name kafka -p 9092:9092 --link zookeeper:zookeeper quay.io/debezium/kafka:2.1
```

## Starting a MySQL database

```sh
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw quay.io/debezium/example-mysql:2.1
```

## Starting a MySQL command line client

```sh
docker run -it --rm --name mysqlterm --link mysql --rm mysql:8.0 sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'

use inventory;
show tables;

SELECT * FROM customers;
```

## Starting Kafka Connect

```sh
docker run -it --rm --name connect -p 8083:8083 -e GROUP_ID=1 -e CONFIG_STORAGE_TOPIC=my_connect_configs -e OFFSET_STORAGE_TOPIC=my_connect_offsets -e STATUS_STORAGE_TOPIC=my_connect_statuses --link kafka:kafka --link mysql:mysql quay.io/debezium/connect:2.1

curl -H "Accept:application/json" localhost:8083/
curl -H "Accept:application/json" localhost:8083/connectors/
```

## Deploying the MySQL connector

### Registering a connector to monitor the inventory database


```json
{
  "name": "inventory-connector",//The name of the connector.
  "config": {//The connector’s configuration.
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",//Only one task should operate at any one time. Because the MySQL connector reads the MySQL server’s binlog, using a single connector task ensures proper order and event handling. The Kafka Connect service uses connectors to start one or more tasks that do the work, and it automatically distributes the running tasks across the cluster of Kafka Connect services. If any of the services stop or crash, those tasks will be redistributed to running services.
    "database.hostname": "mysql",//The database host, which is the name of the Docker container running the MySQL server (mysql). Docker manipulates the network stack within the containers so that each linked container can be resolved with /etc/hosts using the container name for the host name. If MySQL were running on a normal network, you would specify the IP address or resolvable host name for this value.
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.id": "184054",
    "topic.prefix": "dbserver1",//A unique topic prefix. This name will be used as the prefix for all Kafka topics.
    "database.include.list": "inventory",//Only changes in the inventory database will be detected.
    "schema.history.internal.kafka.bootstrap.servers": "kafka:9092",//The connector will store the history of the database schemas in Kafka using this broker (the same broker to which you are sending events) and topic name. Upon restart, the connector will recover the schemas of the database that existed at the point in time in the binlog when the connector should begin reading.
    "schema.history.internal.kafka.topic": "schema-changes.inventory"
  }
}
```

```sh
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "inventory-connector", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "mysql", "database.port": "3306", "database.user": "debezium", "database.password": "dbz", "database.server.id": "184054", "topic.prefix": "dbserver1", "database.include.list": "inventory", "schema.history.internal.kafka.bootstrap.servers": "kafka:9092", "schema.history.internal.kafka.topic": "schemahistory.inventory" } }'

curl -H "Accept:application/json" localhost:8083/connectors/
curl -i -X GET -H "Accept:application/json" localhost:8083/connectors/inventory-connector
```

## Viewing a create event

```sh
docker run -it --rm --name watcher --link zookeeper:zookeeper --link kafka:kafka quay.io/debezium/kafka:2.1 watch-topic -a -k dbserver1.inventory.customers
```

### Updating the database and viewing the update event

```sh
UPDATE customers SET first_name='Anne Marie' WHERE id=1004;
```

## Cleaning up

```sh
docker stop mysqlterm watcher connect mysql kafka zookeeper
```