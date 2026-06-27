# TransactionalBatch - Azure Cosmos DB TransactionalBatch sample

This repository is a small Java sample for building and executing Azure Cosmos DB
`TransactionalBatch` operations. It includes both synchronous and asynchronous
Cosmos DB client examples.

## What it demonstrates

- Creating an immutable Java 25 `Customer` record with a partition key.
- Building transactional batch operations for one logical partition.
- Supporting `CREATE`, `READ`, `REPLACE`, `UPSERT`, and `DELETE` operations from
  command-line arguments.
- Executing the batch through the synchronous Cosmos DB SDK path.
- Keeping the asynchronous Cosmos DB SDK path available in `TxBatch`.
- Unit testing local data generation, immutable copy helpers, and batch
  configuration logic without
  connecting to Cosmos DB.

## Latest updates

- Dependabot `jackson-databind` alerts were addressed by importing the
  `com.fasterxml.jackson:jackson-bom` at `2.22.0`. The Cosmos DB SDK pulls
  Jackson transitively, so the version is constrained in dependency management
  rather than as a direct dependency.
- The sample was refactored for modern Java 25 style:
  - `Customer` is now an immutable `record`.
  - JavaBean-style getters remain for Cosmos/Jackson compatibility.
  - `withCity` and `withRegion` copy helpers preserve immutable updates for
    `REPLACE` and `UPSERT` batch payloads.
  - `TxBatch` uses `var`, `List.of`, `Optional`, switch expressions with arrow
    labels, constants, and parameterized SLF4J logging.
  - Maven compilation uses `<release>25</release>` through the compiler plugin.
- Tests were updated to cover record behavior, immutable copy helpers,
  cumulative batch payload changes, and blank/null unsupported operations.

## Requirements

- Java 25
- Maven 3.x
- An Azure Cosmos DB for NoSQL account when running the sample application

## Project layout

```text
src/main/java/org/example/
  App.java       Application entry point. Calls the synchronous sample path.
  Customer.java  Immutable Java record for sample data and generated test data.
  TxBatch.java   Cosmos DB client setup and TransactionalBatch configuration.

src/test/java/org/example/
  CustomerTest.java  Unit tests for generated customer data and immutable copies.
  TxBatchTest.java   Unit tests for TransactionalBatch operation configuration.
```

## Configure Cosmos DB

Before running the sample application, update these constants in
`src/main/java/org/example/TxBatch.java`:

```java
private static final String ENDPOINT = "Cosmos DB URL";
private static final String KEY = "Access Key";
private static final String DATABASE = "TxDB";
private static final String CONTAINER = "TxContainer";
```

The container is created with `/myPartitionKey` as its partition key path.

## Run unit tests

The unit tests do not require a Cosmos DB account because they only verify local
object generation, immutable copy behavior, and batch construction.

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
