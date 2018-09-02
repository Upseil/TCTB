package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import static com.upseil.game.Config.GridConfigValues.BorderSize;
import static com.upseil.game.Config.GridConfigValues.CellMoveSpeed;
import static com.upseil.game.Config.GridConfigValues.CellSize;
import static com.upseil.game.Config.GridConfigValues.MaxRemovalDelay;
import static com.upseil.game.Config.GridConfigValues.RemovalDuration;
import static com.upseil.game.Config.GridConfigValues.RemovalMoveAmount;
import static com.upseil.game.Config.GridConfigValues.RemovalScaleTo;
import static com.upseil.game.Config.GridConfigValues.Spacing;
import static com.upseil.game.Config.GridConfigValues.TeleportDelay;
import static com.upseil.game.Config.GridConfigValues.TeleportMoveSpeed;

import java.util.Iterator;

import com.artemis.World;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
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
import com.upseil.game.Config.GameConfig;
import com.upseil.game.Config.GridConfig;
import com.upseil.game.Config.GridConfigValues;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Direction;
import com.upseil.game.event.CellsAddedEvent;
import com.upseil.game.event.CellsChangedEvent;
import com.upseil.game.event.CellsRemovedEvent;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.pool.PooledPools;
import com.upseil.gdx.pool.pair.PairPool;
import com.upseil.gdx.pool.pair.PooledPair;
import com.upseil.gdx.util.EnumMap;
import com.upseil.gdx.util.GDXArrays;

// TODO extend AbstractGrid
public class GameGrid extends Group {
    
    private final World world;
    private final Skin skin;
    private final GameGridStyle style;
    private final ExtendedRandom random;
    
    private final Group borderGroup;
    private final EnumMap<Direction, BorderActor> borders;
    
    private final Group cellGroup;
    private final CellActor[][] cells;
    private final Array<ObjectSet<CellActor>> cellsByColor;
    private ObjectFloatMap<CellActor> cellRemovalDelays;
    
    private final PairPool<CellActor, MoveToAction> movementPool;
    private final Array<PooledPair<CellActor, MoveToAction>> queuedMovements;
    private final Array<CellActor> newCells;
    private boolean movementInProgress;
    
    private CellActor blackCell;
    private CellActor whiteCell;
    private float minBlackWhiteDistance;
    private boolean teleportEnabled;
    
    public GameGrid(World world, ExtendedRandom random, float exclusionAreaSize) {
        this.world = world;
        this.skin = world.getRegistered("Skin");
        GameConfig gameConfig = world.getRegistered("Config");
        GridConfig config = gameConfig.getGridConfig();
        this.style = new GameGridStyle(config);
        this.random = random;
        
        int size = config.getInt(GridConfigValues.GridSize);
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
        queuedMovements = new Array<>(false, size, PooledPair.class);
        newCells = new Array<>(false, expectedColorCount, CellActor.class);
        
        initializeGrid(exclusionAreaSize);
        addActor(cellGroup);
        addActor(borderGroup);
    }
    
    // Grid Creation ------------------------------------------------------------------------------
    
    private void initializeBorders() {
        float worldHeight = getWorldHeight();
        float worldWidth = getWorldWidth();
        float borderSize = style.borderSize;
        
        Color borderColor = Color.Black;
        for (Direction direction : Direction.values()) {
            BorderActor actor = new BorderActor(skin, direction, worldWidth, worldHeight, borderSize);
            actor.setBorderColor(borderColor);
            borderGroup.addActor(actor);
            borders.put(direction, actor);
            if (borderColor == Color.Black) {
                borderColor = Color.White;
            } else {
                borderColor = Color.Black;
            }
        }
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
                    createAndSetCell(x, y, Color.random(random));
                }
            }
        }
        teleportEnabled = true;
    }
    
    private CellActor createAndSetCell(int x, int y, Color color) {
        CellActor cell = createCell(x, y, color);
        setCell(x, y, cell);
        return cell;
    }

    private CellActor createCell(int x, int y, Color color) {
        CellActor cell = PooledPools.obtain(CellActor.class).initialize(skin, color, style.cellSize);
        cell.setPosition(toWorld(x), toWorld(y));
        cellGroup.addActor(cell);
        return cell;
    }

    private void setCell(int x, int y, CellActor cell) {
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
                queuedMovements.add(createLinearMoveToAction(cell, toWorld(newX), toWorld(newY)));
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
        teleportEnabled = true;
    }

    private void fillGaps(int startX, int startY, FillGridContext context) {
        int gridX = startX;
        int gridY = startY;
        int spawnX = context.moveLeft() ? getGridWidth()  : context.moveRight() ? -1 : gridX;
        int spawnY = context.moveDown() ? getGridHeight() : context.moveUp()    ? -1 : gridY;
        while (!context.moveToNextLine(gridX, gridY)) {
            CellActor newCell = createCell(spawnX, spawnY, Color.random(random));
            cells[gridX][gridY] = newCell;
            newCells.add(newCell);
            queuedMovements.add(createLinearMoveToAction(newCell, toWorld(gridX), toWorld(gridY)));

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
        PooledPair<CellActor, MoveToAction>[] movements = queuedMovements.items;
        // Iterating over the queued movements backwards, accumulating the
        // needed time for the previous cell to reach the current cell.
        // This looks like cells are "pushed" by their predecessor.
        for (int index = queuedMovements.size - 1; index >= 0; index--) {
            CellActor movingCell = movements[index].getA();
            MoveToAction moveAction = movements[index].getB();
            if (index < queuedMovements.size - 1) {
                CellActor previousCell = movements[index + 1].getA();
                delay += (context.calculateDistance(movingCell, previousCell) - (style.cellOffset * 2)) / style.cellMoveSpeed;
            }
            movingCell.addAction(delay(delay, moveAction));
        }
        
        for (PooledPair<CellActor, MoveToAction> movement : queuedMovements) {
            movement.free();
        }
        queuedMovements.clear();
        movementInProgress = true;
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
        Color[] borderColors = new Color[changeableBorders.size];
        for (int index = 0; index < borderColors.length; index++) {
            borderColors[index] = borders.get(changeableBorders.items[index]).getBorderColor();
        }
        GDXArrays.shuffle(borderColors);
        for (int index = 0; index < borderColors.length; index++) {
            borders.get(changeableBorders.items[index]).setBorderColor(borderColors[index]);
        }
    }
    
    public void abortMovement() {
        if (!isMovementInProgress()) {
            return;
        }
        
        for (ObjectSet<CellActor> cells : cellsByColor) {
            cells.clear();
        }
        newCells.clear();
        teleportEnabled = false;
        
        Array<CellActor> cellsToStop = new Array<>(getGridWidth() * getGridHeight());
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                CellActor cell = cells[x][y];
                if (cell != null && cell.getActions().size > 0) {
                    cellsToStop.add(cell);
                    cells[x][y] = null;
                }
            }
        }
        
        boolean cellsWereRemoved = false;
        for (CellActor cell : cellsToStop) {
            int x = toGrid(cell.getX(Align.center));
            int y = toGrid(cell.getY(Align.center));
            float targetX = toWorld(x);
            float targetY = toWorld(y);
            float duration = Math.max(Math.abs(cell.getX() - targetX), Math.abs(cell.getY() - targetY)) / style.cellMoveSpeed;
            Action action = moveTo(targetX, targetY, duration);
            
            if (isInsideGrid(x, y)) {
                setCell(x, y, cell);
            } else {
                action = sequence(action, Actions.removeActor());
                cellsWereRemoved = true;
            }
            cell.clearActions();
            cell.addAction(action);
        }

        if (cellsWereRemoved) {
            EventSystem.schedule(world, PooledPools.obtain(CellsChangedEvent.class));
        }
    }
    
    public void reset(float exclusionAreaSize) {
        for (ObjectSet<CellActor> cells : cellsByColor) {
            cells.clear();
        }
        GDXArrays.clear(cells);
        cellRemovalDelays.clear();
        queuedMovements.clear();
        newCells.clear();
        cellGroup.clear();
        initializeGrid(exclusionAreaSize);
    }
    
    // Processing ---------------------------------------------------------------------------------
    
    @Override
    public void act(float delta) {
        super.act(delta);
        boolean movementStopped = false;
        if (movementInProgress) {
            movementInProgress = false;
            for (Actor actor : cellGroup.getChildren()) {
                if (actor.getActions().size > 0) {
                    movementInProgress = true;
                    break;
                }
            }
            movementStopped = !movementInProgress;
        }
        
        if (isRemovalInProgress()) {
            processCellRemovalDelays(delta);
        }
        if (isMovementInProgress() || minBlackWhiteDistance < 0) {
            updateMinBlackWhiteDistance();
        }
        if (isMovementInProgress()) {
            processNewCells();
        }

        if (movementStopped && teleportEnabled) {
            checkTeleportation(whiteCell);
            checkTeleportation(blackCell);
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
        boolean isCellNearest = true;

        // Checking if the black or white cell is closer to a border of opposite color
        com.badlogic.gdx.graphics.Color black = skin.getColor(Color.Black.getName());
        for (Direction direction : Direction.values()) { // TODO Adjust after EnumMap implements Iterable
            Actor border = borders.get(direction);
            
            float cellReference = border.getColor().equals(black) ? (direction.isHorizontal() ? whiteCenterX : whiteCenterY)
                                                                  : (direction.isHorizontal() ? blackCenterX : blackCenterY);
            float borderReference = style.borderSize;
            if (border.getX() > 0 || border.getY() > 0) {
                borderReference = (direction.isHorizontal() ? getWorldHeight() : getWorldWidth()) - style.borderSize;
            }
            
            float borderDistance = cellReference - borderReference;
            float borderDistanceSquared = borderDistance * borderDistance;
            if (borderDistanceSquared <= distanceSquared) {
                distanceSquared = borderDistanceSquared;
                isCellNearest = false;
            }
        }
        
        float distanceSurplus = isCellNearest ? style.cellSize + style.cellSpacing
                                              : style.cellOffset + style.cellSize / 2;
        minBlackWhiteDistance = (float) Math.sqrt(distanceSquared) - distanceSurplus;
    }

    private void processNewCells() {
        int cellsAddedCount = 0;
        Iterator<CellActor> newCellsIterator = newCells.iterator();
        while (newCellsIterator.hasNext()) {
            CellActor cell = newCellsIterator.next();
            if (isInsideGrid(cell)) {
                int colorNumber = cell.getCellColor().getNumber();
                if (colorNumber >= 0) {
                    cellsByColor.get(colorNumber).add(cell);
                    cellsAddedCount++;
                }
                newCellsIterator.remove();
            }
        }
        
        if (cellsAddedCount > 0) {
            CellsAddedEvent event = PooledPools.obtain(CellsAddedEvent.class);
            event.setCount(cellsAddedCount);
            EventSystem.schedule(world, event);
        }
    }
    
    private void checkTeleportation(CellActor cell) {
        Color color = cell.getCellColor();
        Direction touchingHorizontalBorder = getTouchingBorder(cell.getX(), true);
        Direction touchingVerticalBorder = getTouchingBorder(cell.getY(), false);
        if ((touchingHorizontalBorder != null && touchingVerticalBorder == null) ||
            (touchingHorizontalBorder == null && touchingVerticalBorder != null)) {
            Direction touchingBorder = touchingHorizontalBorder != null ? touchingHorizontalBorder : touchingVerticalBorder;
            if (borders.get(touchingBorder).getBorderColor() == color) {
                teleport(cell, touchingBorder, getOtherBorder(touchingBorder, color));
            }
        }
    }
    
    private Direction getTouchingBorder(float worldPosition, boolean horizontal) {
        int gridPosition = toGrid(worldPosition);
        if (gridPosition == 0) {
            return horizontal ? Direction.Left : Direction.Bottom;
        }
        if (horizontal && gridPosition == getGridWidth() - 1) {
            return Direction.Right;
        }
        if (!horizontal && gridPosition == getGridHeight() -1) {
            return Direction.Top;
        }
        return null;
    }

    private Direction getOtherBorder(Direction knownDirection, Color color) {
        for (Direction direction : Direction.values()) {
            if (direction != knownDirection && color == borders.get(direction).getBorderColor()) {
                return direction;
            }
        }
        throw new IllegalStateException("No other border for color " + color + " exists: knownDirection = " + knownDirection);
    }
    
    private void teleport(CellActor cell, Direction from, Direction to) {
        float moveDuration = style.paddedCellSize / style.teleportMoveSpeed;
        
        int cellX = toGrid(cell.getX());
        int cellY = toGrid(cell.getY());
        cell.addAction(sequence(delay(style.teleportDelay), moveTo(toWorld(cellX + from.getDeltaX()), toWorld(cellY + from.getDeltaY()), moveDuration), Actions.removeActor()));
        cells[cellX][cellY] = null;
        
        boolean inLine = from.isHorizontal() == to.isHorizontal();
        int targetX = to == Direction.Left ? 0 : to == Direction.Right ? getGridWidth() - 1 : inLine ? cellX : cellY;
        int targetY = to == Direction.Bottom ? 0 : to == Direction.Top ? getGridHeight() - 1 : inLine ? cellY : cellX;
        CellActor newCell = createCell(targetX + to.getDeltaX(), targetY + to.getDeltaY(), cell.getCellColor());
        newCell.addAction((delay(style.teleportDelay, moveTo(toWorld(targetX), toWorld(targetY), moveDuration))));
        
        if (inLine) {
            shiftLine(from.isHorizontal() ? cellY : cellX, from, style.teleportMoveSpeed, style.teleportDelay);
            newCells.add(newCell);
        } else {
            Direction toOpposite = to.getOpposite();
            int removeX = toOpposite == Direction.Left ? 0 : toOpposite == Direction.Right ? getGridWidth() - 1 : targetX;
            int removeY = toOpposite == Direction.Bottom ? 0 : toOpposite == Direction.Top ? getGridHeight() - 1 : targetY;
            cells[removeX][removeY].addAction(sequence(parallel(fadeOut(style.removalDuration, Interpolation.fade),
                                                                scaleTo(style.removalScaleTo, style.removalScaleTo, style.removalDuration, Interpolation.fade),
                                                                Actions.moveBy(0, style.removalMoveAmount, style.removalDuration, Interpolation.pow2In)),
                                              Actions.removeActor()));
            cellsByColor.get(cells[removeX][removeY].getCellColor().getNumber()).remove(cells[removeX][removeY]);
            cells[removeX][removeY] = null;
            CellsRemovedEvent event = PooledPools.obtain(CellsRemovedEvent.class);
            event.setCount(1);
            EventSystem.schedule(world, event);
            shiftLine(to.isHorizontal() ? targetY : targetX, toOpposite, style.teleportMoveSpeed, style.teleportDelay);
            
            Direction fromOpposite = from.getOpposite();
            int fillX = fromOpposite == Direction.Left ? 0 : fromOpposite == Direction.Right ? getGridWidth() - 1 : cellX;
            int fillY = fromOpposite == Direction.Bottom ? 0 : fromOpposite == Direction.Top ? getGridHeight() - 1 : cellY;
            CellActor fillCell = createCell(fillX + fromOpposite.getDeltaX(), fillY + fromOpposite.getDeltaY(), Color.random(random));
            fillCell.addAction(delay(moveDuration + 2 * style.teleportDelay, moveTo(toWorld(fillX), toWorld(fillY), moveDuration)));
            shiftLine(from.isHorizontal() ? cellY : cellX, from, style.teleportMoveSpeed, moveDuration + 2 * style.teleportDelay);
            cells[fillX][fillY] = fillCell;
            newCells.add(fillCell);
        }

        cells[targetX][targetY] = newCell;
        if (cell.getCellColor() == Color.White) {
            whiteCell = newCell;
        }
        if (cell.getCellColor() == Color.Black) {
            blackCell = newCell;
        }
        teleportEnabled = false;
        movementInProgress = true;
    }
    
    private void shiftLine(int number, Direction direction, float movementSpeed, float delay) {
        int cellX = direction ==   Direction.Left ? 0 : direction == Direction.Right ? getGridWidth() - 1  : number;
        int cellY = direction == Direction.Bottom ? 0 : direction ==   Direction.Top ? getGridHeight() - 1 : number;

        float moveDuration = style.paddedCellSize / movementSpeed;
        for (int i = direction.isHorizontal() ? getGridWidth() : getGridHeight(); i > 0; i--) {
            CellActor cell = cells[cellX][cellY];
            int newCellX = cellX + direction.getDeltaX();
            int newCellY = cellY + direction.getDeltaY();
            if (cell != null) {
                MoveToAction moveAction = moveTo(toWorld(newCellX), toWorld(newCellY), moveDuration);
                cell.addAction(delay > 0 ? delay(delay, moveAction) : moveAction);
                cells[newCellX][newCellY] = cell;
            }
            cellX -= direction.getDeltaX();
            cellY -= direction.getDeltaY();
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
    
    public boolean isInsideGrid(int gridX, int gridY) {
        return gridX >= 0 && gridX < getGridWidth() && gridY >= 0 && gridY < getGridHeight();
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
        return movementInProgress;
    }
    
    public GameGridStyle getStyle() {
        return style;
    }
    
    // Utility Classes ----------------------------------------------------------------------------

    public static class GameGridStyle {
        
        public final float borderSize;
        public final float cellSize;
        public final float cellSpacing;
        public final float paddedCellSize;
        public final float cellOffset;
        public final float cellMoveSpeed;
        public final float teleportMoveSpeed;
        public final float teleportDelay;
        
        public final float maxRemovalDelay;
        public final float removalDuration;
        public final float removalMoveAmount;
        public final float removalScaleTo;
        
        public GameGridStyle(GridConfig config) {
            this(config.getFloat(BorderSize), config.getFloat(CellSize), config.getFloat(Spacing), config.getFloat(CellMoveSpeed),
                 config.getFloat(TeleportMoveSpeed), config.getFloat(TeleportDelay), config.getFloat(MaxRemovalDelay),
                 config.getFloat(RemovalDuration), config.getFloat(RemovalMoveAmount), config.getFloat(RemovalScaleTo));
        }

        public GameGridStyle(float borderSize, float cellSize, float spacing, float cellMoveSpeed, float teleportMoveSpeed, float teleportDelay,
                         float maxRemovalDelay, float removalDuration, float removalMoveAmount, float removalScaleTo) {
            this.borderSize = borderSize;
            this.cellSize = cellSize;
            this.cellSpacing = spacing;
            this.paddedCellSize = this.cellSize + spacing;
            this.cellOffset = spacing / 2;
            this.cellMoveSpeed = cellMoveSpeed;
            this.teleportMoveSpeed = teleportMoveSpeed;
            this.teleportDelay = teleportDelay;
            
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
        
        public FillGridContext(Direction moveDirection, int gridWidth, int gridHeight) {
            this.moveDirection = moveDirection;
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
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
        
        public int getStartY() {
            return moveUp() ? gridHeight - 1 : 0;
        }

        public int getStartX() {
            return moveRight() ? gridWidth - 1 : 0;
        }
        
        public int incrementX() {
            return moveDirection.getDeltaX() * -1;
        }
        
        public int incrementY() {
            return moveDirection.getDeltaY() * -1;
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