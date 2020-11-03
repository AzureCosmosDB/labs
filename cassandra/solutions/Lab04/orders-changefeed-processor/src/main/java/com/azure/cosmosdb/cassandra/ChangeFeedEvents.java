package com.azure.cosmosdb.cassandra;

import java.util.ArrayList;
import java.util.List;

public class ChangeFeedEvents {
    private List<ChangeFeedEvent> events = new ArrayList<>();

    public List<ChangeFeedEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ChangeFeedEvent> events) {
        this.events = events;
    }

    public void addEvent(ChangeFeedEvent event) {
        events.add(event);
    }

}