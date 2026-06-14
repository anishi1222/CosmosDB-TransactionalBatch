# TransactionalBatch - Azure Cosmos DB TransactionalBatch sample

This repository is a small Java sample for building and executing Azure Cosmos DB
`TransactionalBatch` operations. It includes both synchronous and asynchronous
Cosmos DB client examples.

## What it demonstrates

- Creating a sample `Customer` item with a partition key.
- Building transactional batch operations for one logical partition.
- Supporting `CREATE`, `READ`, `REPLACE`, `UPSERT`, and `DELETE` operations from
  command-line arguments.
- Executing the batch through the synchronous Cosmos DB SDK path.
- Keeping the asynchronous Cosmos DB SDK path available in `TxBatch`.
- Unit testing the local data generation and batch configuration logic without
  connecting to Cosmos DB.

## Requirements

- Java 25
- Maven 3.x
- An Azure Cosmos DB for NoSQL account when running the sample application

## Project layout

```text
src/main/java/org/example/
  App.java       Application entry point. Calls the synchronous sample path.
  Customer.java  Sample data model and generated test data.
  TxBatch.java   Cosmos DB client setup and TransactionalBatch configuration.

src/test/java/org/example/
  CustomerTest.java  Unit tests for generated customer data and accessors.
  TxBatchTest.java   Unit tests for TransactionalBatch operation configuration.
```

## Configure Cosmos DB

Before running the sample application, update these constants in
`src/main/java/org/example/TxBatch.java`:

```java
final String ENDPOINT = "Cosmos DB URL";
final String KEY = "Access Key";
final String DATABASE = "TxDB";
final String CONTAINER = "TxContainer";
```

The container is created with `/myPartitionKey` as its partition key path.

## Run unit tests

The unit tests do not require a Cosmos DB account because they only verify local
object generation and batch construction.

```bash
mvn test
```

## Build

```bash
mvn package
```

This creates a runnable JAR with dependencies under `target/`.

## Run the sample

After configuring the Cosmos DB endpoint and key, pass one or more supported
operation names to the JAR:

```bash
java -jar target/TransactionalBatch-1.0-SNAPSHOT-jar-with-dependencies.jar CREATE READ REPLACE UPSERT DELETE
```

Supported operation names are case-insensitive:

- `CREATE`
- `READ`
- `REPLACE`
- `UPSERT`
- `DELETE`

`App` currently calls `TxBatch#testBedSync`. The asynchronous path is available
in `TxBatch#testBedAsync` and can be enabled from `App` if needed.
