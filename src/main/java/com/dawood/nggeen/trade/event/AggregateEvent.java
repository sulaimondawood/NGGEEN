package com.dawood.nggeen.trade.event;

import java.util.Collections;
import java.util.List;

public class AggregateEvent {
    private List<DomainEvent> events;

    public void registerEvent(DomainEvent event){
        if(event == null) throw new IllegalArgumentException("Invalid event type");
        events.add(event);
    }

    public List<DomainEvent> getRegisteredEvents(){
        return Collections.unmodifiableList(events);
    }
}
