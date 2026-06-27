package org.example;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CustomerTest {

    @Test
    void createDataPopulatesRequiredFieldsAndUsesIdAsPartitionKey() {
        var customer = Customer.createData();

        assertAll(
                () -> assertNotNull(customer.getId()),
                () -> assertFalse(customer.getId().isBlank()),
                () -> assertDoesNotThrow(() -> UUID.fromString(customer.getId())),
                () -> assertNotNull(customer.getName()),
                () -> assertFalse(customer.getName().isBlank()),
                () -> assertNotNull(customer.getCity()),
                () -> assertFalse(customer.getCity().isBlank()),
                () -> assertNotNull(customer.getRegion()),
                () -> assertFalse(customer.getRegion().isBlank()),
                () -> assertNotNull(customer.getZipCode()),
                () -> assertTrue(customer.getZipCode().matches("\\d{5}")),
                () -> assertNotNull(customer.getUserDefinedId()),
                () -> assertTrue(customer.getUserDefinedId() >= 0),
                () -> assertTrue(customer.getUserDefinedId() <= 1000),
                () -> assertEquals(customer.getId(), customer.getMyPartitionKey()));
    }

    @Test
    void accessorsRoundTripValues() {
        var customer = new Customer(
                "customer-1",
                "Test User",
                "Tokyo",
                "10000",
                "Japan",
                "partition-1",
                42);

        assertAll(
                () -> assertEquals("customer-1", customer.getId()),
                () -> assertEquals("Test User", customer.getName()),
                () -> assertEquals("Tokyo", customer.getCity()),
                () -> assertEquals("10000", customer.getZipCode()),
                () -> assertEquals("Japan", customer.getRegion()),
                () -> assertEquals("partition-1", customer.getMyPartitionKey()),
                () -> assertEquals(42, customer.getUserDefinedId()));
    }

    @Test
    void withMethodsReturnUpdatedCopyWithoutMutatingOriginal() {
        var customer = new Customer(
                "customer-1",
                "Test User",
                "Tokyo",
                "10000",
                "Japan",
                "partition-1",
                42);

        var cityUpdated = customer.withCity("Kyoto");
        var regionUpdated = customer.withRegion("Kansai");

        assertAll(
                () -> assertEquals("Tokyo", customer.getCity()),
                () -> assertEquals("Japan", customer.getRegion()),
                () -> assertEquals("Kyoto", cityUpdated.getCity()),
                () -> assertEquals("Japan", cityUpdated.getRegion()),
                () -> assertEquals("Tokyo", regionUpdated.getCity()),
                () -> assertEquals("Kansai", regionUpdated.getRegion()));
    }
}