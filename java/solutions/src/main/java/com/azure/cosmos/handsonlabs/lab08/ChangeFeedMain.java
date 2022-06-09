package com.azure.cosmos.handsonlabs.lab08;

import com.azure.cosmos.ChangeFeedProcessor;
import com.azure.cosmos.ChangeFeedProcessorBuilder;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.ThroughputProperties;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ChangeFeedMain {

    protected static Logger logger = LoggerFactory.getLogger(ChangeFeedMain.class.getSimpleName());
    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>"; 
    private static CosmosAsyncDatabase storeDatabase;
    private static CosmosAsyncContainer cartContainer;
    private static CosmosAsyncContainer destinationContainer;
    private static CosmosAsyncContainer leaseContainer;

    public static void main(String[] args) {
        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(endpointUri)
                .key(primaryKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();

        storeDatabase = client.getDatabase("StoreDatabase");
        cartContainer = storeDatabase.getContainer("CartContainer");
        destinationContainer = storeDatabase.getContainer("CartContainerByState");
        storeDatabase
                .createContainerIfNotExists("consoleLeases", "/id", ThroughputProperties.createManualThroughput(400))
                .flatMap(containerResponse -> {
                    leaseContainer = storeDatabase.getContainer(containerResponse.getProperties().getId());
                    return Mono.empty();
                }).block();

        ChangeFeedProcessor processor = new ChangeFeedProcessorBuilder()
                .hostName("host_1")
                .feedContainer(cartContainer)
                .leaseContainer(leaseContainer)
                .handleChanges(
                        docs -> {
                            logger.info("Changes received: " + docs.size());
                            Flux.fromIterable(docs).flatMap(doc -> destinationContainer.createItem(doc))
                                    .flatMap(itemResponse -> Mono.empty()).subscribe();
                        })
                .buildChangeFeedProcessor();

        processor.start().subscribe();

        logger.info("Started Change Feed Processor");
        logger.info("Press any key to stop the processor...");

        Scanner input = new Scanner(System.in);
        input.next();
        input.close();

        logger.info("Stopping Change Feed Processor");

        processor.stop().subscribe();
    }
}
