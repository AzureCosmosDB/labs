package com.azure.cosmosdb.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

public class ChangeFeedEvent {
    private String id;
    private int amount;
    private String time;
    private String location;

    public ChangeFeedEvent() {

    }

    public ChangeFeedEvent(String id, int amount, String time, String location) {
        this.id = id;
        this.amount = amount;
        this.time = time;
        this.location = location;
    }

    public static ChangeFeedEvent fromOrder(Row order) {
        String id = order.getUUID("id").toString();
        String amount = order.getString("amount");
        String location = order.getString("location");
        String time = order.getString("time");

        return new ChangeFeedEvent(id, Integer.parseInt(amount), time, location);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}