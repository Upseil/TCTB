package com.upseil.game.domain;

public enum Direction {
    Top, Left, Bottom, Right;
    
    public boolean isHorizontal() {
        return this == Left || this == Right;
    }
    
    public boolean isVertical() {
        return !isHorizontal();
    }
}
