package com.azure.cosmos.handsonlabs.lab08;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lab08Main {
    
    protected static Logger logger = LoggerFactory.getLogger(Lab08Main.class.getSimpleName());
    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>"; 
    public static void main(String[] args) {
        
        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(endpointUri)
                .key(primaryKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();
    }
}
