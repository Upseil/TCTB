package com.upseil.game.domain;

public enum Color {
    Empty(null, -3), Black("black", -2), White("white", -1),
    Color0("t-color0", 0), Color1("t-color1", 1), Color2("t-color2", 2);
    
    private final String name;
    private final int number;

    private Color(String name, int number) {
        this.name = name;
        this.number = number;
    }
    
    public String getName() {
        return name;
    }
    
    public int getNumber() {
        return number;
    }
    
    public static Color forNumber(int number) {
        switch (number) {
        case 0: return Color0;
        case 1: return Color1;
        case 2: return Color2;
        }
        throw new IllegalArgumentException("Number must be between 0 and " + (size() - 1));
    }
    
    public static int size() {
        return 3;
    }

}
