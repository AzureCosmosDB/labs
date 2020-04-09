// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.handsonlabs.lab10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.text.DecimalFormat;

import com.azure.cosmos.ConnectionPolicy;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.handsonlabs.common.datatypes.Food;
import com.azure.cosmos.handsonlabs.common.datatypes.PurchaseFoodOrBeverage;
import com.azure.cosmos.handsonlabs.common.datatypes.ViewMap;
import com.azure.cosmos.handsonlabs.common.datatypes.WatchLiveTelevisionChannel;
import com.azure.cosmos.handsonlabs.common.datatypes.Tag;
import com.azure.cosmos.models.AccessCondition;
import com.azure.cosmos.models.AccessConditionType;
import com.azure.cosmos.models.CosmosAsyncItemResponse;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.IndexingMode;
import com.azure.cosmos.models.IndexingPolicy;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.IncludedPath;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.collect.Lists;

public class Lab10Main {
    protected static Logger logger = LoggerFactory.getLogger(Lab10Main.class.getSimpleName());
    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>";   
    private static CosmosAsyncDatabase database;
    private static CosmosAsyncContainer container;  
    public static void main(String[] args) {
        ConnectionPolicy defaultPolicy = ConnectionPolicy.getDefaultPolicy();
        defaultPolicy.setPreferredLocations(Lists.newArrayList("<your cosmos db account location>"));
    
        CosmosAsyncClient client = new CosmosClientBuilder()
                .setEndpoint(endpointUri)
                .setKey(primaryKey)
                .setConnectionPolicy(defaultPolicy)
                .setConsistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildAsyncClient();

        database = client.getDatabase("NutritionDatabase");
        container = database.getContainer("FoodCollection");

        container.readItem("21083", new PartitionKey("Fast Foods"), Food.class)
            .flatMap(fastFoodResponse -> {
                logger.info("Existing ETag: {}",fastFoodResponse.getResponseHeaders().get("etag"));

                CosmosItemRequestOptions requestOptions = new CosmosItemRequestOptions();
                AccessCondition accessCondition = new AccessCondition();
                accessCondition.setType(AccessConditionType.IF_MATCH);
                accessCondition.setCondition(fastFoodResponse.getResponseHeaders().get("etag"));
                requestOptions.setAccessCondition(accessCondition);

                Food fastFood = fastFoodResponse.getItem();
                fastFood.addTag(new Tag("Demo"));

                CosmosAsyncItemResponse<Food> upsertResponse =
                 container.upsertItem(fastFood,requestOptions).block();

                logger.info("New ETag: {}",upsertResponse.getResponseHeaders().get("etag"));

                fastFood = upsertResponse.getItem();
                fastFood.addTag(new Tag("Failure"));

                try
                {
                    upsertResponse =
                    container.upsertItem(fastFood,requestOptions).block();
                }
                catch (Exception ex)
                {
                    logger.error("Update error",ex);
                }

                return Mono.empty();
        }).block();

        client.close();        
    }
}