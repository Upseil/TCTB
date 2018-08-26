package com.upseil.game.event;

import com.upseil.gdx.event.AbstractEvent;
import com.upseil.gdx.event.EventType;

public class CellsChangedEvent extends AbstractEvent<CellsChangedEvent> {
    
    public static final EventType<CellsChangedEvent> Type = new EventType<>("Cells Changed");
    
    @Override
    protected void setType() {
        this.type = Type;
    }
    
}
