package com.upseil.game.domain;

import com.upseil.gdx.math.ExtendedRandom;

public enum Direction {
    Top(0, 1), Left(-1, 0), Bottom(0, -1), Right(1, 0);
    
    private final int deltaX;
    private final int deltaY;
    
    private Direction(int deltaX, int deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public int getDeltaX() {
        return deltaX;
    }

    public int getDeltaY() {
        return deltaY;
    }

    public boolean isHorizontal() {
        return this == Left || this == Right;
    }
    
    public boolean isVertical() {
        return !isHorizontal();
    }
    
    public Direction getOpposite() {
        switch (this) {
        case Bottom:
            return Top;
        case Left:
            return Right;
        case Right:
            return Left;
        case Top:
            return Bottom;
        }
        throw new IllegalStateException("No opposite available for " + this);
    }
    
    public static Direction random(ExtendedRandom random) {
        Direction[] values = values();
        return values[random.randomIntExclusive(values.length)];
    }
}
