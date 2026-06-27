package org.example;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxBatch {
    private static final String ENDPOINT = "Cosmos DB URL";
    private static final String KEY = "Access Key";
    private static final String DATABASE = "TxDB";
    private static final String CONTAINER = "TxContainer";
    private static final String PARTITION_KEY_PATH = "/myPartitionKey";
    private static final String REPLACEMENT_CITY = "きょうと";
    private static final String UPSERT_REGION = "日本のどこか";
    private static final Logger LOGGER = LoggerFactory.getLogger(TxBatch.class);

    private enum BatchOperation {
        CREATE,
        REPLACE,
        UPSERT,
        DELETE,
        READ;

        static Optional<BatchOperation> from(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException _) {
                return Optional.empty();
            }
        }
    }

    public void testBedSync(Customer targetCustomer, String[] operation) {

        try {
            executeBatch(targetCustomer, operation);
        } catch (CosmosException e) {
            LOGGER.error("Cosmos sync batch execution failed", e);
        }
    }

    public CompletableFuture<Void> testBedAsync(Customer targetCustomer, String[] operation) {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        return CompletableFuture
                .runAsync(() -> executeBatch(targetCustomer, operation), executor)
                .whenComplete((_, throwable) -> {
                    executor.shutdown();
                    if (throwable != null) {
                        LOGGER.error("Cosmos async batch execution failed", throwable);
                    }
                });
    }

    private void executeBatch(Customer targetCustomer, String[] operation) {
        try (CosmosClient client = createClient()) {
            var container = getOrCreateContainer(client);
            var txBatch = configureOperation(targetCustomer, operation);
            if (txBatch.isEmpty()) {
                LOGGER.warn("No batch was executed because an unsupported operation was requested.");
                return;
            }

            var txResponse = container.executeCosmosBatch(txBatch.orElseThrow());
            logBatchResults(txResponse);
        }
    }

    private CosmosClient createClient() {
        return new CosmosClientBuilder()
                .endpoint(ENDPOINT)
                .key(KEY)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .contentResponseOnWriteEnabled(true)
                .preferredRegions(List.of("Japan East"))
                .readRequestsFallbackEnabled(true)
                .endpointDiscoveryEnabled(true)
                .gatewayMode()
                .connectionSharingAcrossClientsEnabled(true)
                .buildClient();
    }

    private CosmosContainer getOrCreateContainer(CosmosClient client) {
        var databaseResponse = client.createDatabaseIfNotExists(DATABASE);
        LOGGER.info(">>[Prepare] Database ready {}", databaseResponse.getProperties().getId());

        var database = client.getDatabase(databaseResponse.getProperties().getId());
        var containerProperties = new CosmosContainerProperties(CONTAINER, PARTITION_KEY_PATH);
        var containerResponse = database.createContainerIfNotExists(containerProperties);
        LOGGER.info(">>[Prepare] Container ready {}", containerResponse.getProperties().getId());
        LOGGER.info(">>[Prepare] requestCharge {}[RU]", containerResponse.getRequestCharge());

        return database.getContainer(containerResponse.getProperties().getId());
    }

    Optional<CosmosBatch> configureOperation(Customer customer, String[] operations) {

        var txBatch = CosmosBatch
                .createCosmosBatch(new PartitionKey(customer.getMyPartitionKey()));
        var currentCustomer = customer;
        for (var requestedOperation : operations) {
            var operation = BatchOperation.from(requestedOperation);
            if (operation.isEmpty()) {
                LOGGER.warn("Unsupported batch operation: {}", requestedOperation);
                return Optional.empty();
            }
            currentCustomer = applyOperation(txBatch, currentCustomer, operation.orElseThrow());
        }

        return Optional.of(txBatch);
    }

    private Customer applyOperation(CosmosBatch txBatch, Customer customer, BatchOperation operation) {
        return switch (operation) {
            case CREATE -> {
                txBatch.createItemOperation(customer);
                yield customer;
            }
            case REPLACE -> {
                var updatedCustomer = customer.withCity(REPLACEMENT_CITY);
                txBatch.replaceItemOperation(updatedCustomer.getId(), updatedCustomer);
                yield updatedCustomer;
            }
            case UPSERT -> {
                var updatedCustomer = customer.withRegion(UPSERT_REGION);
                txBatch.upsertItemOperation(updatedCustomer);
                yield updatedCustomer;
            }
            case DELETE -> {
                txBatch.deleteItemOperation(customer.getId());
                yield customer;
            }
            case READ -> {
                txBatch.readItemOperation(customer.getId());
                yield customer;
            }
        };
    }

    private void logBatchResults(CosmosBatchResponse txResponse) {
        txResponse.getResults().forEach(txResult -> LOGGER.info(
                "Result [{}] - [{}] Operation: {}",
                txResult.getStatusCode(),
                txResult.getSubStatusCode(),
                txResult.getOperation().getOperationType().name()));
    }
}