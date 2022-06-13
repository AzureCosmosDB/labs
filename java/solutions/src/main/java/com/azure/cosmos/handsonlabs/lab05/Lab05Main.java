// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.handsonlabs.lab05;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.cosmos.implementation.ConnectionPolicy;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.handsonlabs.common.datatypes.Food;
import com.azure.cosmos.handsonlabs.common.datatypes.Serving;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import reactor.core.publisher.Mono;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.Lists;

public class Lab05Main {
    protected static Logger logger = LoggerFactory.getLogger(Lab05Main.class.getSimpleName());
    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>";   
    private static CosmosAsyncDatabase database;
    private static CosmosAsyncContainer container;
    private static AtomicInteger pageCount = new AtomicInteger(0);

    public static void main(String[] args) {
        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(endpointUri)
                .key(primaryKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();

        database = client.getDatabase("NutritionDatabase");
        container = database.getContainer("FoodCollection");

        container.readItem("19130", new PartitionKey("Sweets"), Food.class)
                .flatMap(candyResponse -> {
                    Food candy = candyResponse.getItem();
                    logger.info("Read {}", candy.getDescription());
                    return Mono.empty();
                }).block();

        String sqlA = "SELECT f.description, f.manufacturerName, " +
                "f.servings FROM foods f WHERE f.foodGroup = " +
                "'Sweets' and IS_DEFINED(f.description) and " +
                "IS_DEFINED(f.manufacturerName) and IS_DEFINED(f.servings)";

        CosmosQueryRequestOptions optionsA = new CosmosQueryRequestOptions();
        optionsA.setMaxDegreeOfParallelism(1);
        container.queryItems(sqlA, optionsA, Food.class).byPage(100)
                .flatMap(page -> {
                    for (Food fd : page.getResults()) {
                        String msg = "";
                        msg = String.format("%s by %s\n", fd.getDescription(), fd.getManufacturerName());

                        for (Serving sv : fd.getServings()) {
                            msg += String.format("\t%f %s\n", sv.getAmount(), sv.getDescription());
                        }
                        msg += "\n";
                        logger.info(msg);
                    }

                    return Mono.empty();
                }).blockLast();

        String sqlB = "SELECT f.id, f.description, f.manufacturerName, f.servings " +
                "FROM foods f WHERE IS_DEFINED(f.manufacturerName)";

        CosmosQueryRequestOptions optionsB = new CosmosQueryRequestOptions();
        optionsB.setMaxDegreeOfParallelism(5);
        optionsB.setMaxBufferedItemCount(100);
        container.queryItems(sqlB, optionsB, Food.class).byPage()
                .flatMap(page -> {
                    String msg = "";

                    msg = String.format("---Page %d---\n", pageCount.getAndIncrement());

                    for (Food fd : page.getResults()) {
                        msg += String.format("\t[%s]\t%s\t%s\n", fd.getId(), fd.getDescription(),
                                fd.getManufacturerName());
                    }
                    logger.info(msg);
                    return Mono.empty();
                }).blockLast();

        client.close();
    }
}