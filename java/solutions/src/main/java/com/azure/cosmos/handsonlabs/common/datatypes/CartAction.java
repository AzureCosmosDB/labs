package com.azure.cosmos.handsonlabs.common.datatypes;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"_rid", "_self", "_etag", "_attachments", "_lsn", "_ts"})
public class CartAction {

    public String id;
    public int cartId;
    public ActionType action;
    public String item;
    public double price;
    public String buyerState;

    public CartAction() {
        this.id = UUID.randomUUID().toString();
    }

    public CartAction(int cartId, ActionType actionType, String item, double price, String state) {
        this.id = UUID.randomUUID().toString();
        this.cartId = cartId;
        this.action = actionType;
        this.item = item;
        this.price = price;
        this.buyerState = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getBuyerState() {
        return buyerState;
    }

    public void setBuyerState(String buyerState) {
        this.buyerState = buyerState;
    }

}
