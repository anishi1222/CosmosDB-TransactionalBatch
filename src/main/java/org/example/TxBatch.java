package org.example;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;

import java.util.Locale;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    public void testBedAsync(Customer targetCustomer, String[] operation) {

        try (CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(ENDPOINT)
                .key(KEY)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .contentResponseOnWriteEnabled(true)
                .preferredRegions(List.of("Japan East"))
                .readRequestsFallbackEnabled(true)
                .endpointDiscoveryEnabled(true)
                .gatewayMode()
                .connectionSharingAcrossClientsEnabled(true)
                .buildAsyncClient()) {
            var database = client.createDatabaseIfNotExists(DATABASE)
                        .doOnSuccess(response -> {
                            LOGGER.info(">>[Create] Database created {}", response.getProperties().getId());
                        })
                        .flatMap(response -> Mono.just(client.getDatabase(response.getProperties().getId())))
                        .publishOn(Schedulers.boundedElastic())
                        .block();

            var containerProperties = new CosmosContainerProperties(CONTAINER, PARTITION_KEY_PATH);

            var container = database.createContainerIfNotExists(containerProperties)
                    .doOnError(throwable -> LOGGER.info("[Create] Unable to create Container {}", throwable.getMessage()))
                    .doOnSuccess(response -> {
                        LOGGER.info(">>[Create] Container created {}", response.getProperties().getId());
                        LOGGER.info(">>[Create] requestCharge {}[RU]", response.getRequestCharge());
                    })
                    .flatMap(response -> Mono.just(database.getContainer(response.getProperties().getId())))
                    .publishOn(Schedulers.boundedElastic())
                    .block();

            configureOperation(targetCustomer, operation)
                    .map(batch -> container.executeCosmosBatch(batch).block())
                    .ifPresent(this::logBatchResults);
        } catch (CosmosException e) {
            LOGGER.error("Cosmos async batch execution failed", e);
        }
    }

    public void testBedSync(Customer targetCustomer, String[] operation) {

        try (CosmosClient client = new CosmosClientBuilder()
                .endpoint(ENDPOINT)
                .key(KEY)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .contentResponseOnWriteEnabled(true)
                .preferredRegions(List.of("Japan East"))
                .readRequestsFallbackEnabled(true)
                .endpointDiscoveryEnabled(true)
                .gatewayMode()
                .connectionSharingAcrossClientsEnabled(true)
                .buildClient()) {

            var databaseResponse = client.createDatabaseIfNotExists(DATABASE);
            var database = client.getDatabase(databaseResponse.getProperties().getId());
            var containerProperties = new CosmosContainerProperties(CONTAINER, PARTITION_KEY_PATH);

            var containerResponse = database.createContainerIfNotExists(containerProperties);
            var container = database.getContainer(containerResponse.getProperties().getId());

            configureOperation(targetCustomer, operation)
                    .map(container::executeCosmosBatch)
                    .ifPresent(this::logBatchResults);
        } catch (CosmosException e) {
            LOGGER.error("Cosmos sync batch execution failed", e);
        }
    }

    Optional<CosmosBatch> configureOperation(Customer customer, String[] operations) {

        var txBatch = CosmosBatch
                .createCosmosBatch(new PartitionKey(customer.getMyPartitionKey()));
        var currentCustomer = customer;
        for (var requestedOperation : operations) {
            var operation = BatchOperation.from(requestedOperation);
            if (operation.isEmpty()) {
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
                txBatch.readItemOperation(customer.getId()).getItem();
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