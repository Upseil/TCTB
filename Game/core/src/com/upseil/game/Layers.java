package com.upseil.game;

public enum Layers {
    
    Background(-1), HUD(0), World(1), UI(2);
    
    private final int zIndex;

    private Layers(int zIndex) {
        this.zIndex = zIndex;
    }
    
    public int getZIndex() {
        return zIndex;
    }
}
