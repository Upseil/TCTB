package com.upseil.game;

public final class Constants {
    
    public static enum GameInit { 
        Title, Resizable, Width, Height, MinWidth, MinHeight, PrefWidth, PrefHeight
    }

    public static enum Tag {
        GameState, GameScreen, MenuScreen, Grid, HUD, Menu
    }

    public static enum Layers {
        
        Background(-1), HUD(0), World(1), UI(2);
        
        private final int zIndex;
    
        private Layers(int zIndex) {
            this.zIndex = zIndex;
        }
        
        public int getZIndex() {
            return zIndex;
        }
    }
    
    private Constants() { }
    
}
