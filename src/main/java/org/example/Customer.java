package org.example;

import net.datafaker.Faker;

import java.util.Locale;
import java.util.UUID;

public record Customer(
        String id,
        String name,
        String city,
        String zipCode,
        String region,
        String myPartitionKey,
        Integer userDefinedId) {

    private static final Faker JAPANESE_FAKER = new Faker(Locale.of("ja", "JP"));

    public static Customer createData() {
        var id = UUID.randomUUID().toString();
        return new Customer(
                id,
                JAPANESE_FAKER.name().name(),
                JAPANESE_FAKER.country().capital(),
                JAPANESE_FAKER.number().digits(5),
                JAPANESE_FAKER.country().name(),
                id,
                JAPANESE_FAKER.random().nextInt(0, 1000));
    }

    public Customer withCity(String city) {
        return new Customer(id, name, city, zipCode, region, myPartitionKey, userDefinedId);
    }

    public Customer withRegion(String region) {
        return new Customer(id, name, city, zipCode, region, myPartitionKey, userDefinedId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getRegion() {
        return region;
    }

    public String getMyPartitionKey() {
        return myPartitionKey;
    }

    public Integer getUserDefinedId() {
        return userDefinedId;
    }
}
