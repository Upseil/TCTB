package com.upseil.game.domain;

import com.upseil.game.GameApplication;
import com.upseil.gdx.math.ExtendedRandom;

public class Grid {
    
    private final Cell[][] cells;
    
    public Grid(int width, int height, float exclusionAreaSize) {
        cells = new Cell[width][height];
        
        initializeBlackAndWhite(exclusionAreaSize);
        fillGrid();
    }

    private void fillGrid() {
        int width = getWidth();
        int height = getHeight();
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (cells[x][y] == null) {
                    Color color = getRandomCellColor();
                    cells[x][y] = new Cell(x, y, color);
                }
            }
        }
    }

    private void initializeBlackAndWhite(float exclusionAreaSize) {
        int width = getWidth();
        int height = getHeight();
        float exclusionAreaWidth = width * exclusionAreaSize;
        float exclusionAreaHeight = height * exclusionAreaSize;
        float exclusionAreaX = (width - exclusionAreaWidth) / 2;
        float exclusionAreaY = (height - exclusionAreaHeight) / 2;

        int minX = Math.round(exclusionAreaX + exclusionAreaWidth);
        int minY = Math.round(exclusionAreaY + exclusionAreaHeight);
        int maxX = Math.round(exclusionAreaX);
        int maxY = Math.round(exclusionAreaY);
        
        ExtendedRandom random = GameApplication.Random;
        int blackX = random.randomBoolean() ? random.randomInt(0, maxX - 1) : random.randomInt(minX, width - 1);
        int blackY = random.randomBoolean() ? random.randomInt(0, maxY - 1) : random.randomInt(minY, height - 1);
        int whiteX = width - blackX - 1;
        int whiteY = height - blackY - 1;

        cells[blackX][blackY] = new Cell(blackX, blackY, Color.Black);
        cells[whiteX][whiteY] = new Cell(whiteX, whiteY, Color.White);
    }

    private Color getRandomCellColor() {
        float value = GameApplication.Random.randomFloat();
        if (value < 0.333333f) return Color.Color0;
        if (value < 0.666666f) return Color.Color1;
        return Color.Color2;
    }
    
    public Cell getCell(int x, int y) {
        return cells[x][y];
    }

    public void removeCell(int x, int y) {
        getCell(x, y).setColor(Color.Empty);
    }
    
    public int getWidth() {
        return cells.length;
    }
    
    public int getHeight() {
        return cells[0].length;
    }
    
}
