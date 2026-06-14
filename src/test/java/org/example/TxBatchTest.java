package org.example;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import com.azure.cosmos.CosmosItemOperation;
import com.azure.cosmos.CosmosItemOperationType;
import com.azure.cosmos.TransactionalBatch;
import org.junit.jupiter.api.Test;

class TxBatchTest {

    @Test
    void configureOperationBuildsSupportedOperationsInOrder() {
        Customer customer = createCustomer();
        TxBatch txBatch = new TxBatch();

        Optional<TransactionalBatch> configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"CREATE", "READ", "REPLACE", "UPSERT", "DELETE"});

        assertTrue(configuredBatch.isPresent());
        List<CosmosItemOperation> operations = configuredBatch.orElseThrow().getOperations();

        assertEquals(
                List.of(
                        CosmosItemOperationType.CREATE,
                        CosmosItemOperationType.READ,
                        CosmosItemOperationType.REPLACE,
                        CosmosItemOperationType.UPSERT,
                        CosmosItemOperationType.DELETE),
                operations.stream().map(CosmosItemOperation::getOperationType).toList());
    }

    @Test
    void configureOperationUsesCustomerIdForItemOperations() {
        Customer customer = createCustomer();
        TxBatch txBatch = new TxBatch();

        TransactionalBatch configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"READ", "REPLACE", "DELETE"}).orElseThrow();

        List<CosmosItemOperation> operations = configuredBatch.getOperations();

        assertAll(
                () -> assertEquals("customer-1", operations.get(0).getId()),
                () -> assertEquals("customer-1", operations.get(1).getId()),
                () -> assertEquals("customer-1", operations.get(2).getId()));
    }

    @Test
    void configureOperationStoresCustomerForCreateReplaceAndUpsert() {
        Customer customer = createCustomer();
        TxBatch txBatch = new TxBatch();

        TransactionalBatch configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"CREATE", "REPLACE", "UPSERT"}).orElseThrow();

        List<CosmosItemOperation> operations = configuredBatch.getOperations();

        assertAll(
                () -> assertSame(customer, operations.get(0).getItem()),
                () -> assertSame(customer, operations.get(1).getItem()),
                () -> assertSame(customer, operations.get(2).getItem()),
                () -> assertEquals("きょうと", customer.getCity()),
                () -> assertEquals("日本のどこか", customer.getRegion()));
    }

    @Test
    void configureOperationIsCaseInsensitive() {
        Customer customer = createCustomer();
        TxBatch txBatch = new TxBatch();

        TransactionalBatch configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"create", "read"}).orElseThrow();

        assertEquals(
                List.of(CosmosItemOperationType.CREATE, CosmosItemOperationType.READ),
                configuredBatch.getOperations().stream().map(CosmosItemOperation::getOperationType).toList());
    }

    @Test
    void configureOperationReturnsEmptyForUnsupportedOperation() {
        Customer customer = createCustomer();
        TxBatch txBatch = new TxBatch();

        Optional<TransactionalBatch> configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"UNKNOWN"});

        assertFalse(configuredBatch.isPresent());
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setId("customer-1");
        customer.setName("Test User");
        customer.setCity("Tokyo");
        customer.setZipCode("10000");
        customer.setRegion("Japan");
        customer.setMyPartitionKey("customer-1");
        customer.setUserDefinedId(42);
        return customer;
    }
}