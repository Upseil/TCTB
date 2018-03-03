package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.Iterator;

import com.artemis.World;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
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
import com.upseil.game.event.CellsAddedEvent;
import com.upseil.game.event.CellsRemovedEvent;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.pool.PooledPools;
import com.upseil.gdx.pool.pair.PairPool;
import com.upseil.gdx.pool.pair.PooledPair;
import com.upseil.gdx.scene2d.PolygonActor;
import com.upseil.gdx.util.EnumMap;
import com.upseil.gdx.util.GDXArrays;

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
    
    private final PairPool<CellActor, MoveToAction> movementPool;
    private final Array<PooledPair<CellActor, MoveToAction>> cellMovements;
    private final Array<CellActor> newCells;
    
    private CellActor blackCell;
    private CellActor whiteCell;
    private float minBlackWhiteDistance;
    
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
        for (int i = 0; i < Color.size(); i++) {
            cellsByColor.add(new ObjectSet<>(expectedColorCount));
        }
        cellRemovalDelays = new ObjectFloatMap<>(expectedColorCount);
        
        movementPool = new PairPool<>();
        cellMovements = new Array<>(false, config.getGridSize(), PooledPair.class);
        newCells = new Array<>(false, expectedColorCount, CellActor.class);
        
        initializeGrid(config.getExclusionAreaSize());
        addActor(cellGroup);
        addActor(borderGroup);
    }
    
    // Grid Creation ------------------------------------------------------------------------------
    
    private void initializeBorders() {
        float worldHeight = getWorldHeight();
        float worldWidth = getWorldWidth();
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

    private void initializeGrid(float exclusionAreaSize) {
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

        minBlackWhiteDistance = -1;
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
    
    public void fillGrid(Direction moveDirection) {
        FillGridContext context = new FillGridContext(moveDirection, getGridWidth(), getGridHeight());
        int x = context.getStartX();
        int y = context.getStartY();
        int newX = -1;
        int newY = -1;
        while (!context.isGridFilled(x, y)) {
            CellActor cell = cells[x][y];
            // First empty cell -> initialize new position
            if (cell == null && newX < 0 && newY < 0) {
                newX = x;
                newY = y;
            }
            // Non-empty cell with initialized new position -> queue movement
            if (cell != null && newX >= 0 && newY >= 0) {
                cellMovements.add(createLinearMoveToAction(cell, toWorld(newX), toWorld(newY)));
                cells[x][y] = null;
                cells[newX][newY] = cell;
                newX += context.incrementX();
                newY += context.incrementY();
            }
            
            // Increment position and check if done
            x += context.incrementX();
            y += context.incrementY();
            if (context.moveToNextLine(x, y)) {
                // If either newX = -1 or newY = -1, no cells in this line have been removed
                if (newX >= 0 && newY >= 0) {
                    fillGaps(newX, newY, context);
                    applyQueuedMovements(context);
                }

                x = context.nextLineX(x);
                y = context.nextLineY(y);
                newX = -1;
                newY = -1;
            }
        }
    }

    private void fillGaps(int startX, int startY, FillGridContext context) {
        int gridX = startX;
        int gridY = startY;
        int spawnX = context.moveLeft() ? getGridWidth()  : context.moveRight() ? -1 : gridX;
        int spawnY = context.moveDown() ? getGridHeight() : context.moveUp()    ? -1 : gridY;
        while (!context.moveToNextLine(gridX, gridY)) {
            CellActor newCell = createCell(spawnX, spawnY, Color.random(random.asRandom()));
            cells[gridX][gridY] = newCell;
            newCells.add(newCell);
            cellMovements.add(createLinearMoveToAction(newCell, toWorld(gridX), toWorld(gridY)));

            gridX += context.incrementX();
            gridY += context.incrementY();
            spawnX += context.incrementX();
            spawnY += context.incrementY();
        }
    }

    private PooledPair<CellActor, MoveToAction> createLinearMoveToAction(CellActor cell, float targetX, float targetY) {
        float duration = Math.max(Math.abs(cell.getX() - targetX), Math.abs(cell.getY() - targetY)) / style.cellMoveSpeed;
        PooledPair<CellActor, MoveToAction> movement = movementPool.obtain().set(cell, moveTo(targetX, targetY, duration));
        movement.setFreeA(false);
        return movement;
    }
    
    private void applyQueuedMovements(FillGridContext context) {
        float delay = 0;
        PooledPair<CellActor, MoveToAction>[] movements = cellMovements.items;
        // Iterating over the queued movements backwards, accumulating the
        // needed time for the previous cell to reach the current cell.
        // This looks like cells are "pushed" by their predecessor.
        for (int index = cellMovements.size - 1; index >= 0; index--) {
            CellActor movingCell = movements[index].getA();
            MoveToAction moveAction = movements[index].getB();
            if (index < cellMovements.size - 1) {
                CellActor previousCell = movements[index + 1].getA();
                delay += (context.calculateDistance(movingCell, previousCell) - (style.cellOffset * 2)) / style.cellMoveSpeed;
            }
            movingCell.addAction(delay(delay, moveAction));
        }
        
        for (PooledPair<CellActor, MoveToAction> movement : cellMovements) {
            movement.free();
        }
        cellMovements.clear();
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
        if (isRemovalInProgress()) {
            processCellRemovalDelays(delta);
        }
        if (isMovementInProgress() || minBlackWhiteDistance < 0) {
            updateMinBlackWhiteDistance();
        }
        if (isMovementInProgress()) {
            processNewCells();
        }
    }

    private void processCellRemovalDelays(float delta) {
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
            CellsRemovedEvent event = PooledPools.obtain(CellsRemovedEvent.class);
            event.setCount(cellsRemovedCount);
            EventSystem.schedule(world, event);
        }
    }

    private void updateMinBlackWhiteDistance() {
        float blackCenterX = blackCell.getX(Align.center);
        float blackCenterY = blackCell.getY(Align.center);
        float whiteCenterX = whiteCell.getX(Align.center);
        float whiteCenterY = whiteCell.getY(Align.center);

        // Initializing distance with black to white cell distance
        float cellDeltaX = blackCenterX - whiteCenterX;
        float cellDeltaY = blackCenterY - whiteCenterY;
        float distanceSquared = cellDeltaX * cellDeltaX + cellDeltaY * cellDeltaY;

        // Checking if the black or white cell is closer to a border of opposite color
        com.badlogic.gdx.graphics.Color black = skin.getColor(Color.Black.getName());
        for (Direction direction : Direction.values()) { // TODO Adjust after EnumMap implements Iterable
            Actor border = borders.get(direction);
            
            float cellReference = border.getColor().equals(black) ? (direction.isHorizontal() ? blackCenterY : blackCenterX)
                                                                  : (direction.isHorizontal() ? whiteCenterY : whiteCenterX);
            float borderReference = style.borderSize;
            if (border.getX() > 0 || border.getY() > 0) {
                borderReference = (direction.isHorizontal() ? getWorldHeight() : getWorldWidth()) - style.borderSize;
            }
            
            float borderDistance = cellReference - borderReference;
            float borderDistanceSquared = borderDistance * borderDistance;
            if (borderDistanceSquared <= distanceSquared) {
                distanceSquared = borderDistanceSquared;
            }
        }
        
        minBlackWhiteDistance = (float) Math.sqrt(distanceSquared);
    }

    private void processNewCells() {
        int cellsAddedCount = 0;
        Iterator<CellActor> newCellsIterator = newCells.iterator();
        while (newCellsIterator.hasNext()) {
            CellActor cell = newCellsIterator.next();
            if (isInsideGrid(cell)) {
                cellsByColor.get(cell.getCellColor().getNumber()).add(cell);
                newCellsIterator.remove();
                cellsAddedCount++;
            }
        }
        
        if (cellsAddedCount > 0) {
            CellsAddedEvent event = PooledPools.obtain(CellsAddedEvent.class);
            event.setCount(cellsAddedCount);
            EventSystem.schedule(world, event);
        }
    }
    
    // Data Polling -------------------------------------------------------------------------------

    public boolean isInsideGrid(Actor cell) {
        float cellX = cell.getX();
        float cellY = cell.getY();
        float cellMaxX = cellX + cell.getWidth();
        float cellMaxY = cellY + cell.getHeight();
        return cellX >= style.borderSize + style.cellOffset &&
               cellY >= style.borderSize + style.cellOffset &&
               cellMaxX <= getWorldWidth() - style.borderSize - style.cellOffset &&
               cellMaxY <= getWorldHeight() - style.borderSize - style.cellOffset;
    }
    
    public float getMinBlackWhiteDistance() {
        return minBlackWhiteDistance;
    }
    
    public int toGrid(float world) {
        return (int) ((world - style.borderSize) / style.paddedCellSize);
    }
    
    public int getGridWidth() {
        return cells.length;
    }
    
    public int getGridHeight() {
        return cells[0].length;
    }
    
    public float toWorld(int grid) {
        return grid * style.paddedCellSize + style.cellOffset + style.borderSize;
    }

    public float getWorldWidth() {
        return borderGroup.getWidth();
    }

    public float getWorldHeight() {
        return borderGroup.getHeight();
    }
    
    public int getColorCount(Color color) {
        return getColorCount(color.getNumber());
    }
    
    public int getColorCount(int colorNumber) {
        return cellsByColor.get(colorNumber).size;
    }
    
    public boolean isRemovalInProgress() {
        return cellRemovalDelays.size > 0;
    }
    
    public boolean isMovementInProgress() {
        return newCells.size > 0;
    }
    
    public GridStyle getStyle() {
        return style;
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
    
    private static class FillGridContext {
        
        private final Direction moveDirection;
        private final int gridWidth;
        private final int gridHeight;
        
        private final int incrementX;
        private final int incrementY;
        
        public FillGridContext(Direction moveDirection, int gridWidth, int gridHeight) {
            this.moveDirection = moveDirection;
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
            incrementX = moveLeft() ? 1 : moveRight() ? -1 : 0;
            incrementY = moveDown() ? 1 : moveUp()    ? -1 : 0;
        }

        public boolean moveUp() {
            return moveDirection == Direction.Top;
        }
        
        public boolean moveLeft() {
            return moveDirection == Direction.Left;
        }
        
        public boolean moveDown() {
            return moveDirection == Direction.Bottom;
        }
        
        public boolean moveRight() {
            return moveDirection == Direction.Right;
        }
        
        public boolean moveHorizontal() {
            return moveLeft() || moveRight();
        }
        
        public boolean moveVertical() {
            return moveUp() || moveDown();
        }
        
        public int incrementX() {
            return incrementX;
        }
        
        public int incrementY() {
            return incrementY;
        }
        
        public int getStartY() {
            return moveUp() ? gridHeight - 1 : 0;
        }

        public int getStartX() {
            return moveRight() ? gridWidth - 1 : 0;
        }

        public boolean moveToNextLine(int x, int y) {
            return (moveLeft() && x >= gridWidth)  || (moveRight() && x < 0) ||
                   (moveDown() && y >= gridHeight) || (moveUp()    && y < 0);
        }

        public int nextLineX(int currentX) {
            return moveRight() ? gridWidth - 1 : moveLeft() ? 0 : currentX + 1;
        }

        public int nextLineY(int currentY) {
            return moveUp() ? gridHeight - 1 : moveDown() ? 0 : currentY + 1;
        }
        
        public float calculateDistance(CellActor movingCell, CellActor previousCell) {
            float movingCellReference = moveHorizontal() ? movingCell.getX(Align.center)
                                                         : movingCell.getY(Align.center);
            float previousCellReference = moveHorizontal() ? previousCell.getX(Align.center)
                                                           : previousCell.getY(Align.center);
            float cellSize = moveHorizontal() ? movingCell.getWidth() / 2 + previousCell.getWidth() / 2
                                              : movingCell.getHeight() / 2 + previousCell.getHeight() / 2;
            return Math.abs(movingCellReference - previousCellReference) - cellSize;
        }

        public boolean isGridFilled(int x, int y) {
            return (moveHorizontal() && y >= gridHeight) ||
                   (moveVertical()   && x >= gridWidth);
        }
        
    }
    
}