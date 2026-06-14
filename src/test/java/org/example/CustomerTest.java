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
        Customer customer = new Customer().createData();

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
        Customer customer = new Customer();

        customer.setId("customer-1");
        customer.setName("Test User");
        customer.setCity("Tokyo");
        customer.setZipCode("10000");
        customer.setRegion("Japan");
        customer.setMyPartitionKey("partition-1");
        customer.setUserDefinedId(42);

        assertAll(
                () -> assertEquals("customer-1", customer.getId()),
                () -> assertEquals("Test User", customer.getName()),
                () -> assertEquals("Tokyo", customer.getCity()),
                () -> assertEquals("10000", customer.getZipCode()),
                () -> assertEquals("Japan", customer.getRegion()),
                () -> assertEquals("partition-1", customer.getMyPartitionKey()),
                () -> assertEquals(42, customer.getUserDefinedId()));
    }
}