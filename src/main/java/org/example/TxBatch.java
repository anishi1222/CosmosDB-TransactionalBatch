package org.example;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class TxBatch {
    final String ENDPOINT = "Cosmos DB URL";
    final String KEY = "Access Key";
    final String DATABASE = "TxDB";
    final String CONTAINER = "TxContainer";
    static Logger logger;

    public TxBatch() {
        logger = LoggerFactory.getLogger(this.getClass());
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

                CosmosAsyncDatabase database = client.createDatabaseIfNotExists(DATABASE)
                        .doOnError(throwable -> logger.info("[Create] Unable to create Database: " + throwable.getMessage()))
                        .doOnSuccess(response -> {
                            logger.info(">>[Create] Database created " + response .getProperties().getId());
                            logger.info(">>[Create] databaseUsage " + response .getDatabaseUsage() + "[RU]");
                        })
                        .flatMap(response  -> Mono.just(client.getDatabase(response .getProperties().getId())))
                        .publishOn(Schedulers.elastic())
                        .block();

                CosmosContainerProperties containerProperties
                        = new CosmosContainerProperties(CONTAINER, "/myPartitionKey");

                CosmosAsyncContainer container
                        = database.createContainerIfNotExists(containerProperties)
                        .doOnError(throwable -> logger.info("[Create] Unable to create Container " + throwable.getMessage()))
                        .doOnSuccess(response-> {
                            logger.info(">>[Create] Container created " + response.getProperties().getId());
                            logger.info(">>[Create] requestCharge " + response.getRequestCharge() + "[RU]");
                        })
                        .flatMap(response -> Mono.just(database.getContainer(response.getProperties().getId())))
                        .publishOn(Schedulers.elastic())
                        .block();

            Optional<TransactionalBatch> txBatch = configureOperation(targetCustomer, operation);
            if(txBatch.isPresent()) {
                TransactionalBatchResponse txResponse = container.executeTransactionalBatch(txBatch.get()).block();
                txResponse.getResults().forEach(txResult -> logger.info(
                        "Result [" + txResult.getStatusCode() +
                                "] - [" + txResult.getSubStatusCode() +
                                "] Operation: " + txResult.getOperation().getOperationType().name()));
            }
        } catch (CosmosException e) {
            e.printStackTrace();
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

            CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(DATABASE);
            CosmosDatabase database = client.getDatabase(databaseResponse.getProperties().getId());
            CosmosContainerProperties containerProperties
                    = new CosmosContainerProperties(CONTAINER, "/myPartitionKey");

            CosmosContainerResponse containerResponse = database.createContainerIfNotExists(containerProperties);
            CosmosContainer container = database.getContainer(containerResponse.getProperties().getId());

            Optional<TransactionalBatch> txBatch = configureOperation(targetCustomer, operation);
            if(txBatch.isPresent()) {
                TransactionalBatchResponse txResponse = container.executeTransactionalBatch(txBatch.get());
                txResponse.getResults().forEach(txResult -> logger.info(
                        "Result [" + txResult.getStatusCode() +
                                "] - [" + txResult.getSubStatusCode() +
                                "] Operation: " + txResult.getOperation().getOperationType().name()));
            }
        } catch (CosmosException e) {
            e.printStackTrace();
        }
    }

    Optional<TransactionalBatch> configureOperation(Customer _customer, String[] _operation) {

        TransactionalBatch txBatch = TransactionalBatch
                .createTransactionalBatch(new PartitionKey(_customer.getMyPartitionKey()));
        for (String s : _operation) {
            switch (s.toUpperCase()) {
                case "CREATE":
                    // add Create Operation
                    txBatch.createItemOperation(_customer);
                    break;
                case "REPLACE":
                    // add Replace Operation
                    _customer.setCity("きょうと");
                    txBatch.replaceItemOperation(_customer.getId(), _customer);
                    break;
                case "UPSERT":
                    // add Upsert Operation
                    _customer.setRegion("日本のどこか");
                    txBatch.upsertItemOperation(_customer);
                    break;
                case "DELETE":
                    // add Delete Operation
                    txBatch.deleteItemOperation(_customer.getId());
                    break;
                case "READ":
                    // add read operation
                    txBatch.readItemOperation(_customer.getId()).getItem();
                    break;
                default:
                    txBatch = null;
                    break;
            }
        }

        return Optional.ofNullable(txBatch);
    }
}