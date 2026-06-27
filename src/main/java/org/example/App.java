package org.example;

public class App {
    public static void main(String[] args) {
        var targetCustomer = Customer.createData();
        var txBatch = new TxBatch();
        txBatch.testBedSync(targetCustomer, args);
    }
}
