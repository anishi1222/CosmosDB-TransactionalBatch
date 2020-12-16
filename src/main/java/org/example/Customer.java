package org.example;

import com.github.javafaker.Faker;
import java.util.Locale;
import java.util.UUID;

public class Customer {
    String id;
    String name;
    String city;
    String zipCode;
    String region;
    String myPartitionKey;
    Integer userDefinedId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getMyPartitionKey() {
        return myPartitionKey;
    }

    public void setMyPartitionKey(String myPartitionKey) {
        this.myPartitionKey = myPartitionKey;
    }

    public Integer getUserDefinedId() {
        return userDefinedId;
    }

    public void setUserDefinedId(Integer userDefinedId) {
        this.userDefinedId = userDefinedId;
    }

    public Customer createData() {

        Faker faker = new Faker(new Locale("ja_JP"));
        Customer sampleCustomer = new Customer();
        sampleCustomer.setCity(faker.country().capital());
        sampleCustomer.setUserDefinedId(faker.random().nextInt(0, 1000));
        sampleCustomer.setZipCode(faker.number().digits(5));
        sampleCustomer.setName(faker.name().name());
        sampleCustomer.setId(UUID.randomUUID().toString());
        sampleCustomer.setRegion(faker.country().name());
        sampleCustomer.setMyPartitionKey(sampleCustomer.getId());
        return sampleCustomer;
    }
}
