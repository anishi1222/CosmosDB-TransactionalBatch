package org.example;

public class App 
{
    public static void main( String[] args )
    {
        // Initialize
        Customer targetCustomer = new Customer().createData();

        TxBatch txBatch = new TxBatch();
        txBatch.testBedSync(targetCustomer,args);

//        txBatch.testBedAsync(targetCustomer,args);

    }
}
