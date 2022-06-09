package com.azure.cosmos.handsonlabs.lab08;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.handsonlabs.common.datatypes.ActionType;
import com.azure.cosmos.handsonlabs.common.datatypes.CartAction;
import com.azure.cosmos.handsonlabs.common.datatypes.StateCount;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Azure Functions with HTTP Trigger.
 */
public class MaterializedViewFunction {

    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>";
    private String databaseId = "StoreDatabase";
    private String containerId = "StateSales";
    protected static Logger logger = LoggerFactory.getLogger(Lab08Main.class.getSimpleName());
    private CosmosAsyncClient client;

    public MaterializedViewFunction() {
        client = new CosmosClientBuilder()
                .endpoint(endpointUri)
                .key(primaryKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();
    }

    @FunctionName("MaterializedViewFunction")
    public void cosmosDbProcessor(
            @CosmosDBTrigger(name = "MaterializedView", databaseName = "StoreDatabase", collectionName = "CartContainerByState", createLeaseCollectionIfNotExists = true, leaseCollectionName = "materializedViewLeases", connectionStringSetting = "AzureCosmosDBConnection") String[] items,
            final ExecutionContext context) {

        if (items != null && items.length > 0) {
            logger.info("Documents modified " + items.length);
        }

        Map<String, List<Double>> stateMap = new HashMap<String, List<Double>>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (String doc : items) {
            try {
                CartAction cartAction = objectMapper.readValue(doc, CartAction.class);
                if (cartAction.action != ActionType.Purchased) {
                    continue;
                }
                if (!stateMap.containsKey(cartAction.buyerState)) {
                    stateMap.put(cartAction.buyerState, new ArrayList<Double>());
                }
                stateMap.get(cartAction.buyerState).add(cartAction.price);
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        CosmosAsyncDatabase database = client.getDatabase(databaseId);
        CosmosAsyncContainer container = database.getContainer(containerId);

        Flux.fromIterable(stateMap.keySet())
                .flatMap(key -> {
                    String query = "select * from StateSales s where s.State ='" + key + "'";
                    return container.queryItems(query, new CosmosQueryRequestOptions(), StateCount.class)
                            .byPage(1)
                            .flatMap(page -> {
                                if (!page.getResults().isEmpty()) {
                                    StateCount stateCount = page.getResults().get(0);
                                    logger.info("found item with state: " + stateCount.getState());
                                    stateCount.totalSales += stateMap.get(key).stream().reduce(0.0, (a, b) -> a + b);
                                    stateCount.count += stateMap.get(key).size();
                                    return Mono.just(stateCount);
                                } else {
                                    StateCount stateCount = new StateCount();
                                    stateCount.state = key;
                                    stateCount.totalSales = stateMap.get(key).stream().reduce(0.0, (a, b) -> a + b);
                                    stateCount.count = stateMap.get(key).size();
                                    return Mono.just(stateCount);
                                }
                            }).flatMap(item -> {
                                logger.info("upsert item with state: " + item.getState());
                                return container.upsertItem(item);
                            });
                }).collectList().block();
    }
}
