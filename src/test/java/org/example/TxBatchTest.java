package org.example;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.azure.cosmos.models.CosmosItemOperation;
import com.azure.cosmos.models.CosmosItemOperationType;

class TxBatchTest {

    @Test
    void configureOperationBuildsSupportedOperationsInOrder() {
        var customer = createCustomer();
        var txBatch = new TxBatch();

        var configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"CREATE", "READ", "REPLACE", "UPSERT", "DELETE"});

        assertTrue(configuredBatch.isPresent());
        var operations = configuredBatch.orElseThrow().getOperations();

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
        var customer = createCustomer();
        var txBatch = new TxBatch();

        var configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"READ", "REPLACE", "DELETE"}).orElseThrow();

        var operations = configuredBatch.getOperations();

        assertAll(
                () -> assertEquals("customer-1", operations.get(0).getId()),
                () -> assertEquals("customer-1", operations.get(1).getId()),
                () -> assertEquals("customer-1", operations.get(2).getId()));
    }

    @Test
    void configureOperationStoresCustomerForCreateReplaceAndUpsert() {
        var customer = createCustomer();
        var txBatch = new TxBatch();

        var configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"CREATE", "REPLACE", "UPSERT"}).orElseThrow();

        var operations = configuredBatch.getOperations();
        var replacementCustomer = (Customer) operations.get(1).getItem();
        var upsertCustomer = (Customer) operations.get(2).getItem();

        assertAll(
                () -> assertEquals(customer, operations.get(0).getItem()),
                () -> assertEquals("Tokyo", customer.getCity()),
                () -> assertEquals("Japan", customer.getRegion()),
                () -> assertEquals("きょうと", replacementCustomer.getCity()),
                () -> assertEquals("Japan", replacementCustomer.getRegion()),
                () -> assertEquals("きょうと", upsertCustomer.getCity()),
                () -> assertEquals("日本のどこか", upsertCustomer.getRegion()));
    }

    @Test
    void configureOperationIsCaseInsensitive() {
        var customer = createCustomer();
        var txBatch = new TxBatch();

        var configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"create", "read"}).orElseThrow();

        assertEquals(
                List.of(CosmosItemOperationType.CREATE, CosmosItemOperationType.READ),
                configuredBatch.getOperations().stream().map(CosmosItemOperation::getOperationType).toList());
    }

    @Test
    void configureOperationReturnsEmptyForUnsupportedOperation() {
        var customer = createCustomer();
        var txBatch = new TxBatch();

        var configuredBatch = txBatch.configureOperation(
                customer,
                new String[] {"UNKNOWN"});

        assertFalse(configuredBatch.isPresent());
    }

    @Test
    void configureOperationReturnsEmptyForBlankOrNullOperation() {
        var customer = createCustomer();
        var txBatch = new TxBatch();

        assertAll(
                () -> assertFalse(txBatch.configureOperation(customer, new String[] {"  "}).isPresent()),
                () -> assertFalse(txBatch.configureOperation(customer, new String[] {null}).isPresent()));
    }

    private Customer createCustomer() {
        return new Customer(
                "customer-1",
                "Test User",
                "Tokyo",
                "10000",
                "Japan",
                "customer-1",
                42);
    }
}