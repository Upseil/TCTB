package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.artemis.World;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectFloatMap.Entries;
import com.badlogic.gdx.utils.ObjectFloatMap.Entry;
import com.badlogic.gdx.utils.ObjectSet;
import com.upseil.game.GameApplication;
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.GridConfig;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Direction;
import com.upseil.game.event.CellRemovalEvent;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.pool.PooledPools;
import com.upseil.gdx.scene2d.PolygonActor;
import com.upseil.gdx.util.EnumMap;
import com.upseil.gdx.util.GDXArrays;

// TODO Gap filling in arbitrary direction, resetting
public class GridActor extends Group {
    
    private final World world;
    private final Skin skin;
    private final GridStyle style;
    private final ExtendedRandom random;
    
    private final Group borderGroup;
    private final EnumMap<Direction, Actor> borders;
    
    private final Group cellGroup;
    private final CellActor[][] cells;
    private final Array<ObjectSet<CellActor>> cellsByColor;
    private ObjectFloatMap<CellActor> cellRemovalDelays;
    
    private CellActor blackCell;
    private CellActor whiteCell;
    
    public GridActor(World world, ExtendedRandom random) {
        this.world = world;
        this.skin = world.getRegistered("Skin");
        GameConfig gameConfig = world.getRegistered("Config");
        GridConfig config = gameConfig.getGridConfig();
        this.style = new GridStyle(config);
        this.random = random;
        
        int size = config.getGridSize();
        float worldSize = size * style.paddedCellSize + 2 * style.borderSize;
        int expectedColorCount = (size * size) / Color.size();
        
        borderGroup = new Group();
        borderGroup.setBounds(0, 0, worldSize, worldSize);
        borders = new EnumMap<>(Direction.class);
        initializeBorders();
        
        cellGroup = new Group();
        cellGroup.setBounds(0, 0, worldSize, worldSize);
        cells = new CellActor[size][size];
        cellsByColor = new Array<>(Color.size());
        for (int i = 0; i < cellsByColor.size; i++) {
            cellsByColor.add(new ObjectSet<>(expectedColorCount));
        }
        cellRemovalDelays = new ObjectFloatMap<>(expectedColorCount);
        fillGrid(config.getExclusionAreaSize());
        
        addActor(cellGroup);
        addActor(borderGroup);
    }
    
    // Grid Creation ------------------------------------------------------------------------------
    
    private void initializeBorders() {
        float worldHeight = borderGroup.getHeight();
        float worldWidth = borderGroup.getWidth();
        float borderSize = style.borderSize;
        
        float[] vertices = new float[] { 0,worldHeight,  borderSize,worldHeight-borderSize,  worldWidth-borderSize,worldHeight-borderSize,  worldWidth,worldHeight };
        Actor topBorder = new PolygonActor(vertices);
        topBorder.setColor(skin.getColor(Color.Black.getName()));
        topBorder.setPosition(0, worldHeight - borderSize);
        borderGroup.addActor(topBorder);
        borders.put(Direction.Top, topBorder);
        
        vertices = new float[] { 0,worldHeight,  0,0,  borderSize,borderSize,  borderSize,worldHeight-borderSize };
        Actor leftBorder = new PolygonActor(vertices);
        leftBorder.setPosition(0, 0);
        leftBorder.setColor(skin.getColor(Color.White.getName()));
        borderGroup.addActor(leftBorder);
        borders.put(Direction.Left, leftBorder);
        
        vertices = new float[] { borderSize,borderSize,  0,0,  worldWidth,0,  worldWidth-borderSize,borderSize };
        Actor bottomBorder = new PolygonActor(vertices);
        bottomBorder.setPosition(0, 0);
        bottomBorder.setColor(skin.getColor(Color.Black.getName()));
        borderGroup.addActor(bottomBorder);
        borders.put(Direction.Bottom, bottomBorder);
        
        vertices = new float[] { worldWidth-borderSize,worldHeight-borderSize,  worldWidth-borderSize,borderSize,  worldWidth,0,  worldWidth,worldHeight };
        Actor rightBorder = new PolygonActor(vertices);
        rightBorder.setPosition(worldWidth - borderSize, 0);
        rightBorder.setColor(skin.getColor(Color.White.getName()));
        borderGroup.addActor(rightBorder);
        borders.put(Direction.Right, rightBorder);
    }

    private void fillGrid(float exclusionAreaSize) {
        int width = getGridWidth();
        int height = getGridHeight();
        float exclusionAreaWidth = width * exclusionAreaSize;
        float exclusionAreaHeight = height * exclusionAreaSize;
        float exclusionAreaX = (width - exclusionAreaWidth) / 2;
        float exclusionAreaY = (height - exclusionAreaHeight) / 2;

        int minX = Math.round(exclusionAreaX + exclusionAreaWidth);
        int minY = Math.round(exclusionAreaY + exclusionAreaHeight);
        int maxX = Math.round(exclusionAreaX);
        int maxY = Math.round(exclusionAreaY);
        
        ExtendedRandom random = GameApplication.Random;
        int blackX = random.randomBoolean() ? random.randomInt(1, maxX - 1) : random.randomInt(minX, width - 2);
        int blackY = random.randomBoolean() ? random.randomInt(1, maxY - 1) : random.randomInt(minY, height - 2);
        int whiteX = width - blackX - 1;
        int whiteY = height - blackY - 1;

        blackCell = createAndSetCell(blackX, blackY, Color.Black);
        whiteCell = createAndSetCell(whiteX, whiteY, Color.White);
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                if (cells[x][y] == null) {
                    createAndSetCell(x, y, Color.random(random.asRandom()));
                }
            }
        }
    }
    
    private CellActor createAndSetCell(int x, int y, Color color) {
        CellActor cell = createCell(x, y, color);
        setCell(x, y, cell);
        return cell;
    }

    public CellActor createCell(int x, int y, Color color) {
        CellActor cell = PooledPools.obtain(CellActor.class).initialize(skin, color, style.cellSize);
        cell.setPosition(toWorld(x), toWorld(y));
        cellGroup.addActor(cell);
        return cell;
    }

    public void setCell(int x, int y, CellActor cell) {
        cells[x][y] = cell;
        int colorNumber = cell.getCellColor().getNumber();
        if (colorNumber >= 0) {
            cellsByColor.get(colorNumber).add(cell);
        }
    }
    
    // Modifications and Interactions -------------------------------------------------------------
    
    public void removeCells(Color color) {
        ObjectSet<CellActor> cellsOfColor = cellsByColor.get(color.getNumber());
        if (cellsOfColor.size == 0) return;
        
        float duration = style.removalDuration;
        float scaleTo = style.removalScaleTo;
        float moveAmount = style.removalMoveAmount;
        for (CellActor cell : cellsOfColor) {
            float removalDelay = random.randomFloat(0, style.maxRemovalDelay);
            int x = toGrid(cell.getX());
            int y = toGrid(cell.getY());
            
            cells[x][y] = null;
            cellRemovalDelays.put(cell, removalDelay);

            cell.toFront();
            cell.addAction(sequence(delay(removalDelay),
                                     parallel(fadeOut(duration, Interpolation.fade),
                                              scaleTo(scaleTo, scaleTo, duration, Interpolation.fade),
                                              Actions.moveBy(0, moveAmount, duration, Interpolation.pow2In)),
                                     Actions.removeActor()));
        }
    }

    public void randomizeBorderColors() {
        // Prevent color changes of borders that are "touched" by the black or white cell
        Direction[] directions = Direction.values();
        Array<Direction> changeableBorders = new Array<>(false, directions, 0, directions.length);

        int blackX = toGrid(blackCell.getX());
        int blackY = toGrid(blackCell.getY());
        if (blackX == 0) changeableBorders.removeValue(Direction.Left, true);
        if (blackX == getGridWidth() - 1) changeableBorders.removeValue(Direction.Right, true);
        if (blackY == 0) changeableBorders.removeValue(Direction.Bottom, true);
        if (blackY == getGridHeight() - 1) changeableBorders.removeValue(Direction.Top, true);

        int whiteX = toGrid(whiteCell.getX());
        int whiteY = toGrid(whiteCell.getY());
        if (whiteX == 0) changeableBorders.removeValue(Direction.Left, true);
        if (whiteX == getGridWidth() - 1) changeableBorders.removeValue(Direction.Right, true);
        if (whiteY == 0) changeableBorders.removeValue(Direction.Bottom, true);
        if (whiteY == getGridHeight() - 1) changeableBorders.removeValue(Direction.Top, true);
        
        // Shuffling the colors of the changeable borders
        com.badlogic.gdx.graphics.Color[] borderColors = new com.badlogic.gdx.graphics.Color[changeableBorders.size];
        for (int index = 0; index < borderColors.length; index++) {
            borderColors[index] = borders.get(changeableBorders.items[index]).getColor().cpy();
        }
        GDXArrays.shuffle(borderColors);
        for (int index = 0; index < borderColors.length; index++) {
            borders.get(changeableBorders.items[index]).setColor(borderColors[index]);
        }
    }
    
    // Processing ---------------------------------------------------------------------------------
    
    @Override
    public void act(float delta) {
        super.act(delta);
        if (cellRemovalDelays.size > 0) {
            processCellRemovalDelays(delta);
        }
    }

    public void processCellRemovalDelays(float delta) {
        int cellsRemovedCount = 0;
        Entries<CellActor> cellsToRemove = cellRemovalDelays.entries();
        while (cellsToRemove.hasNext()) {
            Entry<CellActor> entry = cellsToRemove.next();
            if (entry.value <= delta) {
                cellsToRemove.remove();
                cellsRemovedCount++;
                
                Color cellColor = entry.key.getCellColor();
                cellsByColor.get(cellColor.getNumber()).remove(entry.key);
            } else {
                cellRemovalDelays.put(entry.key, entry.value - delta);
            }
        }
        
        if (cellsRemovedCount > 0) {
            CellRemovalEvent event = PooledPools.obtain(CellRemovalEvent.class);
            event.setCount(cellsRemovedCount);
            EventSystem.schedule(world, event);
        }
    }
    
    // Data Acquisition ---------------------------------------------------------------------------
    
    private float toWorld(int grid) {
        return grid * style.paddedCellSize + style.cellOffset + style.borderSize;
    }
    
    private int toGrid(float stage) {
        return (int) ((stage - style.borderSize) / style.paddedCellSize);
    }
    
    public int getGridWidth() {
        return cells.length;
    }
    
    public int getGridHeight() {
        return cells[0].length;
    }
    
    public int getColorCount(Color color) {
        return getColorCount(color.getNumber());
    }
    
    public int getColorCount(int colorNumber) {
        return cellsByColor.get(colorNumber).size;
    }
    
    public boolean isCellRemovalInProgress() {
        return cellRemovalDelays.size > 0;
    }
    
    // Utility Classes ----------------------------------------------------------------------------

    public static class GridStyle {
        
        public final float borderSize;
        public final float cellSize;
        public final float paddedCellSize;
        public final float cellOffset;
        public final float cellMoveSpeed;
        
        public final float maxRemovalDelay;
        public final float removalDuration;
        public final float removalMoveAmount;
        public final float removalScaleTo;
        
        public GridStyle(GridConfig config) {
            this(config.getBorderSize(), config.getCellSize(), config.getSpacing(), config.getCellMoveSpeed(),
                 config.getMaxRemovalDelay(), config.getRemovalDuration(), config.getRemovalMoveAmount(), config.getRemovalScaleTo());
        }

        public GridStyle(float borderSize, float cellSize, float spacing, float cellMoveSpeed,
                         float maxRemovalDelay, float removalDuration, float removalMoveAmount, float removalScaleTo) {
            this.borderSize = borderSize;
            this.cellSize = cellSize;
            paddedCellSize = this.cellSize + spacing;
            cellOffset = spacing / 2;
            this.cellMoveSpeed = cellMoveSpeed;
            
            this.maxRemovalDelay = maxRemovalDelay;
            this.removalDuration = removalDuration;
            this.removalMoveAmount = removalMoveAmount;
            this.removalScaleTo = removalScaleTo;
        }
        
    }
    
}
