package com.upseil.game.domain;

public enum Color implements ValueEnum {
    Color1(0), Color2(1), Color3(3);
    
    private final int value;

    private Color(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(int value) {
        return this.value == value;
    }
    
}
