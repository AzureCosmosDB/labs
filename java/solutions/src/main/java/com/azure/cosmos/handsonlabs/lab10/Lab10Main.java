// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.handsonlabs.lab10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.handsonlabs.common.datatypes.Food;
import com.azure.cosmos.handsonlabs.common.datatypes.Tag;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;

import reactor.core.publisher.Mono;

public class Lab10Main {
    protected static Logger logger = LoggerFactory.getLogger(Lab10Main.class.getSimpleName());
    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>";
    private static CosmosAsyncDatabase database;
    private static CosmosAsyncContainer container;

    public static void main(String[] args) {

        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(endpointUri)
                .key(primaryKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();

        database = client.getDatabase("NutritionDatabase");
        container = database.getContainer("FoodCollection");

        container.readItem("21083", new PartitionKey("Fast Foods"), Food.class)
                .flatMap(fastFoodResponse -> {
                    logger.info("Existing ETag: {}", fastFoodResponse.getResponseHeaders().get("etag"));
                    CosmosItemRequestOptions requestOptions = new CosmosItemRequestOptions();
                    requestOptions.setIfMatchETag(fastFoodResponse.getResponseHeaders().get("etag"));
                    Food fastFood = fastFoodResponse.getItem();
                    fastFood.addTag(new Tag("Demo"));
                    CosmosItemResponse<Food> upsertResponse = container.upsertItem(fastFood, requestOptions).block();
                    logger.info("New ETag: {}", upsertResponse.getResponseHeaders().get("etag"));

                    fastFood = upsertResponse.getItem();
                    fastFood.addTag(new Tag("Failure"));

                    try {
                        upsertResponse = container.upsertItem(fastFood, requestOptions).block();
                    } catch (Exception ex) {
                        logger.error("Update error", ex);
                    }
                    return Mono.empty();
                }).block();

        client.close();
    }
}