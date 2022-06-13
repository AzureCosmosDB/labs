package com.azure.cosmos.handsonlabs.lab08;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.handsonlabs.common.datatypes.ActionType;
import com.azure.cosmos.handsonlabs.common.datatypes.CartAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;

public class DataGenerator {

    protected static Logger logger = LoggerFactory.getLogger(DataGenerator.class.getSimpleName());
    private static String endpointUri = "<your uri>";
    private static String primaryKey = "<your key>"; 

    private static CosmosAsyncDatabase storeDatabase;
    private static CosmosAsyncContainer cartContainer;

    private static Random random = new Random();

    private static String[] items = new String[] {
            "Unisex Socks", "Women's Earring", "Women's Necklace", "Unisex Beanie",
            "Men's Baseball Hat", "Unisex Gloves", "Women's Flip Flop Shoes", "Women's Silver Necklace",
            "Men's Black Tee", "Men's Black Hoodie", "Women's Blue Sweater", "Women's Sweatpants",
            "Men's Athletic Shorts", "Women's Athletic Shorts", "Women's White Sweater", "Women's Green Sweater",
            "Men's Windbreaker Jacket", "Women's Sandal", "Women's Rainjacket", "Women's Denim Shorts",
            "Men's Fleece Jacket", "Women's Denim Jacket", "Men's Walking Shoes", "Women's Crewneck Sweater",
            "Men's Button-Up Shirt", "Women's Flannel Shirt", "Women's Light Jeans", "Men's Jeans",
            "Women's Dark Jeans", "Women's Red Top", "Men's White Shirt", "Women's Pant", "Women's Blazer Jacket",
            "Men's Puffy Jacket",
            "Women's Puffy Jacket", "Women's Athletic Shoes", "Men's Athletic Shoes", "Women's Black Dress",
            "Men's Suit Jacket", "Men's Suit Pant",
            "Women's High Heel Shoe", "Women's Cardigan Sweater", "Men's Dress Shoes", "Unisex Puffy Jacket",
            "Women's Red Dress", "Unisex Scarf",
            "Women's White Dress", "Unisex Sandals", "Women's Bag"
    };

    private static String[] states = new String[] {
            "AL", "AK", "AS", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FM", "FL", "GA", "GU", "HI", "ID", "IL",
            "IN",
            "IA", "KS", "KY", "LA", "ME", "MH", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM",
            "NY", "NC", "ND", "MP", "OH", "OK", "OR", "PW", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UT", "VT",
            "VI",
            "VA", "WA", "WV", "WI", "WY"
    };

    private static double[] prices = new double[] {
            3.75, 8.00, 12.00, 10.00,
            17.00, 20.00, 14.00, 15.50,
            9.00, 25.00, 27.00, 21.00, 22.50,
            22.50, 32.00, 30.00, 49.99, 35.50,
            55.00, 50.00, 65.00, 31.99, 79.99,
            22.00, 19.99, 19.99, 80.00, 85.00,
            90.00, 33.00, 25.20, 40.00, 87.50, 99.99,
            95.99, 75.00, 70.00, 65.00, 92.00, 95.00,
            72.00, 25.00, 120.00, 105.00, 130.00, 29.99,
            84.99, 12.00, 37.50
    };

    public static void main(String[] args) {

        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(endpointUri)
                .key(primaryKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();

        storeDatabase = client.getDatabase("StoreDatabase");
        cartContainer = storeDatabase.getContainer("CartContainer");

        logger.info("Enter number of documents to generate.");
        
        Scanner scanner = new Scanner(System.in);
        int noOfDocuments = scanner.nextInt();
        scanner.close();

        List<CartAction> cartActions = new ArrayList<CartAction>();
        for (int i = 0; i < noOfDocuments; i++) {
            cartActions.addAll(generateActions());
        }

        Flux<CartAction> cartActionFlux = Flux.fromIterable(cartActions);

        cartActionFlux.flatMap(item -> {
            return cartContainer.createItem(item);
        }).collectList().block();

        client.close();
    }

    private static List<CartAction> generateActions() {

        List<CartAction> actions = new ArrayList<CartAction>();

        int itemIndex = random.nextInt(items.length);
        int stateIndex = random.nextInt(states.length);
        int cartId = (random.nextInt(99999 - 1000) + 1000);
        ActionType actionType = ActionType.values()[random.nextInt(ActionType.values().length)];
        CartAction cartAction = new CartAction(cartId, actionType, items[itemIndex], prices[itemIndex],
                states[stateIndex]);

        if (cartAction.action != ActionType.Viewed) {
            List<ActionType> previousActions = new ArrayList<ActionType>();
            previousActions.add(ActionType.Viewed);

            if (cartAction.action == ActionType.Purchased) {
                previousActions.add(ActionType.Added);
            }

            previousActions.forEach(previousAction -> {
                CartAction previous = new CartAction(cartAction.cartId, previousAction, cartAction.item,
                        cartAction.price, cartAction.buyerState);
                actions.add(previous);
            });
        }

        actions.add(cartAction);
        return actions;
    }

}
