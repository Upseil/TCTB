package com.upseil.game.event;

import com.upseil.gdx.event.AbstractEvent;
import com.upseil.gdx.event.EventType;

public class CellsAddedEvent extends AbstractEvent<CellsAddedEvent> {
    
    public static EventType<CellsAddedEvent> Type = new EventType<>("Cells Added");
    
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    protected void setType() {
        this.type = Type;
    }
    
    @Override
    public void reset() {
        super.reset();
        count = 0;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append(": ").append(count);
        return builder.toString();
    }
    
}
