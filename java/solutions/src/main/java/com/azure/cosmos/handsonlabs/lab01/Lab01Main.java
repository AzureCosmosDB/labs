package com.azure.cosmos.handsonlabs.lab01;

import com.azure.cosmos.handsonlabs.common.datatypes.ViewMap;
import com.azure.cosmos.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.javafaker.Faker;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class Lab01Main {
    protected static Logger logger = LoggerFactory.getLogger(Lab01Main.class.getSimpleName());
    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>";   
    private static CosmosAsyncDatabase targetDatabase;
    private static CosmosAsyncContainer customContainer;
    private static AtomicBoolean resourcesCreated = new AtomicBoolean(false);

    public static void main(String[] args) {

        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(endpointUri)
                .key(primaryKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();

        // Async resource creation
        client.createDatabaseIfNotExists("EntertainmentDatabase").flatMap(databaseResponse -> {
            targetDatabase = client.getDatabase(databaseResponse.getProperties().getId());
            IndexingPolicy indexingPolicy = new IndexingPolicy();
            indexingPolicy.setIndexingMode(IndexingMode.CONSISTENT);
            indexingPolicy.setAutomatic(true);
            List<IncludedPath> includedPaths = new ArrayList<>();
            IncludedPath includedPath = new IncludedPath("/*");
            includedPaths.add(includedPath);
            indexingPolicy.setIncludedPaths(includedPaths); 

            CosmosContainerProperties containerProperties = 
                new CosmosContainerProperties("CustomContainer", "/type");
            containerProperties.setIndexingPolicy(indexingPolicy);
            return targetDatabase.createContainerIfNotExists(containerProperties, ThroughputProperties.createManualThroughput(400));
        }).flatMap(containerResponse -> {
            customContainer = targetDatabase.getContainer(containerResponse.getProperties().getId());
            return Mono.empty();
        }).subscribe(voidItem -> {}, err -> {}, () -> {
            resourcesCreated.set(true);
        });
    
        while (!resourcesCreated.get());

        logger.info("Database Id:\t{}",targetDatabase.getId());
        logger.info("Container Id:\t{}",customContainer.getId()); 

        targetDatabase = client.getDatabase("EntertainmentDatabase");
        customContainer = targetDatabase.getContainer("CustomContainer");

        ArrayList<ViewMap> mapInteractions = new ArrayList<ViewMap>();
        Faker faker = new Faker();

        for (int i= 0; i < 500;i++){  
            ViewMap doc = new ViewMap(); 

            doc.setMinutesViewed(faker.random().nextInt(1, 60));
            doc.setType("WatchLiveTelevisionChannel");
            doc.setId(UUID.randomUUID().toString());
            mapInteractions.add(doc);
        }

        Flux<ViewMap> mapInteractionsFlux = Flux.fromIterable(mapInteractions);
        List<CosmosItemResponse<ViewMap>> results = 
            mapInteractionsFlux.flatMap(interaction -> customContainer.createItem(interaction)).collectList().block();

        results.forEach(result -> logger.info("Item Created\t{}",result.getItem().getId()));

        client.close();
    }
}
