package com.upseil.game.system;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.Iterator;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectFloatMap.Entries;
import com.badlogic.gdx.utils.ObjectFloatMap.Entry;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Scaling;
import com.upseil.game.GameApplication;
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.GridConfig;
import com.upseil.game.Layers;
import com.upseil.game.Tag;
import com.upseil.game.component.GameState;
import com.upseil.game.domain.Color;
import com.upseil.game.scene2d.CellActor;
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.event.ResizeEvent;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.pool.PooledPools;
import com.upseil.gdx.pool.pair.PairPool;
import com.upseil.gdx.pool.pair.PooledPair;
import com.upseil.gdx.scene2d.PolygonActor;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.util.GDXArrays;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialWorldViewport;

public class GridController extends BaseSystem {
    
    private TagManager<Tag> tagManager;
    private LayeredSceneRenderSystem<?> renderSystem;
    private ComponentMapper<GameState> gameStateMapper;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenPadding;
    private Scene gridScene;
    private float borderSize;
    private float cellSize;
    private float paddedCellSize;
    private float offset;
    private float slowMoThresholdFactor;
    private float minSlowMoTimeScale;
    
    private GameState gameState;
    
    private boolean updateScreenSize;
    private boolean initializeGrid;
    private boolean lost;
    private float grayness;
    
    private Actor borders[];
    private CellActor[][] cells;
    private Array<ObjectSet<CellActor>> cellsByColor;
    private CellActor blackCell;
    private final Vector2 previousBlackCellPosition = new Vector2();
    private CellActor whiteCell;
    private final Vector2 previousWhiteCellPosition = new Vector2();
    
    private ObjectFloatMap<CellActor> cellRemovalDelays;
    private Color colorToRemove;
    private float maxRemovalDelay;
    private float removalDuration;
    private float removalMoveAmount;
    private float removalScaleTo;
    
    private PairPool<CellActor, MoveToAction> movementPool;
    private Array<PooledPair<CellActor, MoveToAction>> cellMovements;
    private Array<CellActor> newCells;
    private float cellMoveSpeed;
    
    @Override
    protected void initialize() {
        world.getSystem(EventSystem.class).registerHandler(ResizeEvent.Type, e -> updateScreenSize = true);
        
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getGridConfig();
        maxRemovalDelay = config.getMaxRemovalDelay();
        removalDuration = config.getRemovalDuration();
        removalMoveAmount = config.getRemovalMoveAmount();
        removalScaleTo = config.getRemovalScaleTo();
        cellMoveSpeed = config.getCellMoveSpeed();
        slowMoThresholdFactor = config.getSlowMoThresholdFactor();
        minSlowMoTimeScale = config.getMinSlowMoTimeScale();
        
        float worldSize = config.getGridSize() * (config.getCellSize() + config.getSpacing()) + 2 * config.getBorderSize();
        screenPadding = new PaddedScreen();
        PartialWorldViewport gridViewport = new PartialScalingViewport(screenPadding, Scaling.fit, worldSize, worldSize);
        Stage gridStage = new Stage(gridViewport, world.getSystem(LayeredSceneRenderSystem.class).getGlobalBatch());
        
        Entity gridEntity = world.createEntity();
        EntityEdit gridEdit = gridEntity.edit();
        gridEdit.create(Layer.class).setZIndex(Layers.World.getZIndex());
        gridEdit.create(InputHandler.class).setProcessor(gridStage);
        gridScene = gridEdit.create(Scene.class);
        gridScene.initialize(gridStage);//.setTimeScale(0.1f);
        tagManager.register(Tag.Grid, gridEntity);

        int gridSize = config.getGridSize();
        borders = new Actor[4];
        cells = new CellActor[gridSize][gridSize];
        int expectedColorCount = getExpectedColorCount();
        cellsByColor = new Array<>(true, Color.size(), ObjectSet.class);
        for (int number = 0; number < Color.size(); number++) {
            cellsByColor.add(new ObjectSet<>(expectedColorCount));
        }
        cellRemovalDelays = new ObjectFloatMap<>(expectedColorCount);
        movementPool = new PairPool<>();
        cellMovements = new Array<>(true, config.getGridSize(), PooledPair.class);
        newCells = new Array<>(true, expectedColorCount, CellActor.class);
        
        initializeGrid = true;
    }

    public int getExpectedColorCount() {
        int expectedColorCount = (getGridWidth() * getGridHeight()) / Color.size();
        return expectedColorCount;
    }

    public Sprite createSpriteForColor(int colorNumber) {
        Color color = Color.forNumber(colorNumber);
        TextureRegion texture = skin.get("t-dot", TextureRegion.class);
        Sprite sprite = new Sprite(texture);
        sprite.setColor(skin.getColor(color.getName()));
        return sprite;
    }
    
    @Override
    protected void begin() {
        gameState = gameStateMapper.get(tagManager.getEntityId(Tag.GameState));
    }
    
    @Override
    protected void processSystem() {
        if (updateScreenSize) {
            updateScreenSize();
            updateScreenSize = false;
        }
        
        if (initializeGrid) {
            gridScene.clear();
            clearHelperStructures();
            initializeGrid();
            gridScene.setTimeScale(1);
            GameApplication.HUD.setButtonsDisabled(false);
            initializeGrid = false;
        }
        
        if (cellRemovalDelays.size > 0) {
            processRemovalDelays();
        }
        if (colorToRemove != null) {
            removeCells();
            colorToRemove = null;
        }
        
        if (newCells.size > 0) {
            processNewCells();
            checkBlackAndWhiteCells();
            if (newCells.size == 0 && !lost) {
                gridScene.setTimeScale(1);
                randomizeBorderColors();
                GameApplication.HUD.setUpdateValueLabels(true);
                GameApplication.HUD.setContinousUpdate(false);
                GameApplication.HUD.setButtonsDisabled(false);
            }
        }
        
        if (lost && grayness < 1) {
            grayness += world.delta;
            ShaderProgram shader = renderSystem.getGlobalBatch().getShader(); 
            int attribute = shader.getAttributeLocation("a_grayness");
            Gdx.gl20.glVertexAttrib1f(attribute, Math.min(grayness, 1));
        }
    }

    private void randomizeBorderColors() {
        // Prevent color changes of borders that are "touched" by the black or white cell
        Array<Actor> changeableBorders = new Array<>(false, borders, 0, borders.length);

        int blackX = toGrid(blackCell.getX());
        int blackY = toGrid(blackCell.getY());
        if (blackX == 0) changeableBorders.removeValue(getLeftBorder(), true);
        if (blackX == getGridWidth() - 1) changeableBorders.removeValue(getRightBorder(), true);
        if (blackY == 0) changeableBorders.removeValue(getBottomBorder(), true);
        if (blackY == getGridHeight() - 1) changeableBorders.removeValue(getTopBorder(), true);

        int whiteX = toGrid(whiteCell.getX());
        int whiteY = toGrid(whiteCell.getY());
        if (whiteX == 0) changeableBorders.removeValue(getLeftBorder(), true);
        if (whiteX == getGridWidth() - 1) changeableBorders.removeValue(getRightBorder(), true);
        if (whiteY == 0) changeableBorders.removeValue(getBottomBorder(), true);
        if (whiteY == getGridHeight() - 1) changeableBorders.removeValue(getTopBorder(), true);
        
        // Shuffling the colors of the changeable borders
        com.badlogic.gdx.graphics.Color[] borderColors = new com.badlogic.gdx.graphics.Color[changeableBorders.size];
        for (int index = 0; index < borderColors.length; index++) {
            borderColors[index] = changeableBorders.items[index].getColor().cpy();
        }
        GDXArrays.shuffle(borderColors);
        for (int index = 0; index < borderColors.length; index++) {
            changeableBorders.items[index].setColor(borderColors[index]);
        }
    }

    public void clearHelperStructures() {
        for (int number = 0; number < Color.size(); number++) {
            cellsByColor.get(number).clear();
        }
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                CellActor cell = cells[x][y];
                if (cell != null) {
                    cell.clearActions();
                    cells[x][y] = null;
                }
            }
        }
        cellRemovalDelays.clear();
        cellMovements.clear();
        newCells.clear();
    }

    // TODO Interpolate sudden changes of the time scale (black and white cell stop moving or are close together and start moving)
    // TODO Apply time scaling only if it's relevant (e.g. distance gets smaller)
    private void checkBlackAndWhiteCells() {
        boolean blackOrWhiteMoving = !previousBlackCellPosition.epsilonEquals(blackCell.getX(), blackCell.getY()) ||
                                     !previousWhiteCellPosition.epsilonEquals(whiteCell.getX(), whiteCell.getY());
        if (!blackOrWhiteMoving) {
            gridScene.setTimeScale(1);
            return;
        }
        previousBlackCellPosition.set(blackCell.getX(), blackCell.getY());
        previousWhiteCellPosition.set(whiteCell.getX(), whiteCell.getY());
        
        
        // Initializing check values with black to white cell distance
        float slowMoThresholdSquared = paddedCellSize * slowMoThresholdFactor * paddedCellSize * slowMoThresholdFactor;
        float blackCenterX = blackCell.getX(Align.center);
        float blackCenterY = blackCell.getY(Align.center);
        float whiteCenterX = whiteCell.getX(Align.center);
        float whiteCenterY = whiteCell.getY(Align.center);
        
        float deltaX = blackCenterX - whiteCenterX;
        float deltaY = blackCenterY - whiteCenterY;
        float distanceSquared = deltaX * deltaX + deltaY * deltaY;
        float loseDistance = cellSize + offset * 2;
        float loseEpsilon = cellSize / 20;
        
        // Checking if the black or white cell is closer to a border of opposite color
        com.badlogic.gdx.graphics.Color black = skin.getColor(Color.Black.getName());
        for (Actor border : borders) {
            boolean horizontal = border.getWidth() > border.getHeight();
            boolean high = border.getX() > 0 || border.getY() > 0;
            
            float valueToCheck = border.getColor().equals(black) ? (horizontal ? whiteCenterY : whiteCenterX)
                                                                 : (horizontal ? blackCenterY : blackCenterX);
            float borderReference = !high ? borderSize : (horizontal ? gridScene.getHeight() : gridScene.getWidth()) - borderSize;
            float borderDistance = valueToCheck - borderReference;
            float borderDistanceSquared = borderDistance * borderDistance;
            if (borderDistanceSquared <= distanceSquared) {
                distanceSquared = borderDistanceSquared;
                loseDistance = offset + (cellSize / 2);
                loseEpsilon = cellSize / 15;
            }
        }
        
        if (distanceSquared <= slowMoThresholdSquared) {
            float timeScale = distanceSquared / slowMoThresholdSquared;
            gridScene.setTimeScale(Math.max(minSlowMoTimeScale, timeScale));
            
            // TODO This is not robust against big delta times
             if (Math.abs(distanceSquared - (loseDistance * loseDistance)) <= (loseEpsilon * loseEpsilon)) {
                // TODO Proper state flow
                gridScene.setPaused(true);
                clearHelperStructures();
                lost = true;
                
//                initializeGrid = true;
//                gameState.setScore(0);
            }
        }
    }

    public void processNewCells() {
        Iterator<CellActor> newCellsIterator = newCells.iterator();
        while (newCellsIterator.hasNext()) {
            CellActor cell = newCellsIterator.next();
            boolean isInsideGrid = isInsideGrid(cell);
            if (isInsideGrid) {
                cellsByColor.get(cell.getCellColor().getNumber()).add(cell);
                newCellsIterator.remove();
            }
        }
    }

    public boolean isInsideGrid(CellActor cell) {
        float cellMaxX = cell.getX() + cell.getWidth();
        float cellMaxY = cell.getY() + cell.getHeight();
        return cell.getX() >= borderSize + offset && cell.getY() >= borderSize + offset &&
               cellMaxX <= gridScene.getWidth() - borderSize - offset &&
               cellMaxY <= gridScene.getHeight() - borderSize - offset;
    }

    public void processRemovalDelays() {
        float sceneDelta = world.delta * gridScene.getActiveTimeScale();
        Entries<CellActor> cellsToRemove = cellRemovalDelays.entries();
        while (cellsToRemove.hasNext()) {
            Entry<CellActor> entry = cellsToRemove.next();
            if (entry.value <= sceneDelta) {
                cellsToRemove.remove();
                gameState.incrementScore();
                
                Color cellColor = entry.key.getCellColor();
                ObjectSet<CellActor> cells = cellsByColor.get(cellColor.getNumber());
                cells.remove(entry.key);
                if (cells.size == 0) {
                    fillGaps(cellColor);
                }
            } else {
                cellRemovalDelays.put(entry.key, entry.value - sceneDelta);
            }
        }
    }

    @SuppressWarnings("incomplete-switch")
    private void fillGaps(Color colorRemoved) {
        int x = colorRemoved == Color.Color2 ? getGridWidth() - 1 : 0;
        int y = 0;
        int newX = -1;
        int newY = -1;
        boolean done = false;
        
        while (!done) {
            CellActor cell = cells[x][y];
            // First empty cell -> new position
            if (cell == null && newX < 0 && newY < 0) {
                newX = x;
                newY = y;
            } 
            // First non-empty cell after a new position has been found -> Move cell
            if (cell != null && newX >= 0 && newY >= 0) {
                float stageX = toStage(newX);
                float stageY = toStage(newY);
                float duration = Math.max(Math.abs(cell.getX() - stageX), Math.abs(cell.getY() - stageY)) / cellMoveSpeed;
                PooledPair<CellActor, MoveToAction> movement = movementPool.obtain().set(cell, moveTo(stageX, stageY, duration));
                movement.setFreeA(false);
                cellMovements.add(movement);
                // This cell is now empty -> update new position
                cells[newX][newY] = cell;
                newX += colorRemoved == Color.Color1 ? 0 :
                        colorRemoved == Color.Color0 ? 1 : -1;
                newY += colorRemoved == Color.Color1 ? 1 : 0;
            }
            
            // Increment positions depending on the movement direction
            boolean nextLine = false;
            switch (colorRemoved) {
            case Color0:
                x++;
                nextLine = x >= getGridWidth();
                if (nextLine) {
                    x = 0;
                    y++;
                    done = y >= getGridHeight();
                }
                break;
            case Color1:
                y++;
                nextLine = y >= getGridHeight();
                if (nextLine) {
                    y = 0;
                    x++;
                    done = x >= getGridWidth();
                }
                break;
            case Color2:
                x--;
                nextLine = x < 0;
                if (nextLine) {
                    x = getGridWidth() - 1;
                    y++;
                    done = y >= getGridHeight();
                }
                break;
            }
            
            if (nextLine) {
                // If either newX = -1 or newY = -1, no cells in this line have been removed
                if (newX >= 0 && newY >= 0) {
                    int newCellsCount = colorRemoved == Color.Color0 ? getGridWidth() - newX :
                                        colorRemoved == Color.Color1 ? getGridHeight() - newY : newX + 1;
                    for (int i = 0; i < newCellsCount; i++) {
                        // Create new cells outside of the viewport
                        int spawnX = colorRemoved == Color.Color0 ? newX + newCellsCount :
                                     colorRemoved == Color.Color1 ? newX: newX - newCellsCount;
                        int spawnY = colorRemoved == Color.Color1 ? newY + newCellsCount : newY;
                        
                        CellActor newCell = createCell(spawnX, spawnY, getRandomCellColor(), cellSize);
                        // But add them at their correct position of the logical grid
                        cells[newX][newY] = newCell;
                        newCells.add(newCell);
                        
                        // Enqueue the new cells movement action, so that it's synced with the actions of the remaining cells
                        float stageX = toStage(newX);
                        float stageY = toStage(newY);
                        float duration = Math.max(Math.abs(newCell.getX() - stageX), Math.abs(newCell.getY() - stageY)) / cellMoveSpeed;
                        PooledPair<CellActor, MoveToAction> movement = movementPool.obtain().set(newCell, moveTo(stageX, stageY, duration));
                        movement.setFreeA(false);
                        cellMovements.add(movement);
    
                        newX += colorRemoved == Color.Color1 ? 0 :
                                colorRemoved == Color.Color0 ? 1 : -1;
                        newY += colorRemoved == Color.Color1 ? 1 : 0;
                    }
                    
                    float delay = 0;
                    PooledPair<CellActor, MoveToAction>[] movements = cellMovements.items;
                    // Iterating over the queued movements backwards, accumulating the needed for the previous cell to reach the current cell
                    // This leads to the illusion that one cell is "pushed" by the cell before.
                    for (int index = cellMovements.size - 1; index >= 0; index--) {
                        CellActor movingCell = movements[index].getA();
                        MoveToAction moveAction = movements[index].getB();
                        if (index < cellMovements.size - 1) {
                            CellActor previousCell = movements[index + 1].getA();
                            float cellReference = colorRemoved == Color.Color1 ? movingCell.getY() + movingCell.getHeight() :
                                                                                 movingCell.getX() + (colorRemoved == Color.Color0 ? movingCell.getWidth() : 0);
                            float previousCellReference = colorRemoved == Color.Color1 ? previousCell.getY() :
                                                                                         previousCell.getX() + (colorRemoved == Color.Color0 ? 0 : previousCell.getWidth());
                            delay += (Math.abs(cellReference - previousCellReference) - (offset * 2)) / cellMoveSpeed;
                        }
                        movingCell.addAction(delay(delay, moveAction));
                    }
                    
                    for (PooledPair<CellActor, MoveToAction> movement : cellMovements) {
                        movement.free();
                    }
                    cellMovements.clear();
                }
                
                newX = -1;
                newY = -1;
            }
        }
        
        // Final steps after the gap filling has been setup
        
        // Ensure the borders are drawn on top of the new cells
        for (Actor border : borders) {
            border.toFront();
        }
        
        // Initializing the black and white cell movement detection
        previousBlackCellPosition.set(blackCell.getX(), blackCell.getY());
        previousWhiteCellPosition.set(whiteCell.getX(), whiteCell.getY());
    }

    public void removeCells() {
        ExtendedRandom random = GameApplication.Random;
        ObjectSet<CellActor> cellsToRemove = cellsByColor.get(colorToRemove.getNumber());
        for (CellActor cell : cellsToRemove) {
            float removalDelay = random.randomFloat(0, maxRemovalDelay);
            int x = toGrid(cell.getX());
            int y = toGrid(cell.getY());
            
            cells[x][y] = null;
            cellRemovalDelays.put(cell, removalDelay);

            cell.toFront();
            cell.addAction(sequence(delay(removalDelay),
                                     parallel(fadeOut(removalDuration, Interpolation.fade),
                                              scaleTo(removalScaleTo, removalScaleTo, removalDuration, Interpolation.fade),
                                              moveBy(0, removalMoveAmount, removalDuration, Interpolation.pow2In)),
                                     removeActor()));
        }
        GameApplication.HUD.setContinousUpdate(true);
    }

    public void updateScreenSize() {
        int padding = config.getGridPadding();
        int top = Math.round(GameApplication.HUD.getTopHeight()) + padding;
        int left = Math.round(GameApplication.HUD.getLeftWidth()) + padding;
        int bottom = Math.round(GameApplication.HUD.getBottomHeight()) + padding;
        int right = Math.round(GameApplication.HUD.getRightWidth()) + padding;
        
        screenPadding.pad(top, left, bottom, right);
        gridScene.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void initializeGrid() {
        borderSize = config.getBorderSize();
        cellSize = config.getCellSize();
        paddedCellSize = cellSize + config.getSpacing();
        offset = config.getSpacing() / 2;

        initializeGridFrame();
        initializeBlackAndWhite(config.getExclusionAreaSize(), cellSize);
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                if (cells[x][y] == null) {
                    createAndSetCell(x, y, getRandomCellColor(), cellSize);
                }
            }
        }
    }

    private void initializeGridFrame() {
        float worldWidth = gridScene.getWidth();
        float worldHeight = gridScene.getHeight();
        
        float[] vertices = new float[] { 0,worldHeight,  borderSize,worldHeight-borderSize,  worldWidth-borderSize,worldHeight-borderSize,  worldWidth,worldHeight };
        Actor topBorder = new PolygonActor(vertices);
        topBorder.setColor(skin.getColor(Color.Black.getName()));
        topBorder.setPosition(0, worldHeight - borderSize);
        gridScene.addActor(topBorder);
        borders[0] = topBorder;
        
        vertices = new float[] { 0,worldHeight,  0,0,  borderSize,borderSize,  borderSize,worldHeight-borderSize };
        Actor leftBorder = new PolygonActor(vertices);
        leftBorder.setPosition(0, 0);
        leftBorder.setColor(skin.getColor(Color.White.getName()));
        gridScene.addActor(leftBorder);
        borders[1] = leftBorder;
        
        vertices = new float[] { borderSize,borderSize,  0,0,  worldWidth,0,  worldWidth-borderSize,borderSize };
        Actor bottomBorder = new PolygonActor(vertices);
        bottomBorder.setPosition(0, 0);
        bottomBorder.setColor(skin.getColor(Color.Black.getName()));
        gridScene.addActor(bottomBorder);
        borders[2] = bottomBorder;
        
        vertices = new float[] { worldWidth-borderSize,worldHeight-borderSize,  worldWidth-borderSize,borderSize,  worldWidth,0,  worldWidth,worldHeight };
        Actor rightBorder = new PolygonActor(vertices);
        rightBorder.setPosition(worldWidth - borderSize, 0);
        rightBorder.setColor(skin.getColor(Color.White.getName()));
        gridScene.addActor(rightBorder);
        borders[3] = rightBorder;
    }
    
    private Actor getTopBorder() {
        return borders[0];
    }
    
    private Actor getLeftBorder() {
        return borders[1];
    }
    
    private Actor getBottomBorder() {
        return borders[2];
    }
    
    private Actor getRightBorder() {
        return borders[3];
    }

    private void initializeBlackAndWhite(float exclusionAreaSize, float cellSize) {
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

        blackCell = createAndSetCell(blackX, blackY, Color.Black, cellSize);
        whiteCell = createAndSetCell(whiteX, whiteY, Color.White, cellSize);
    }
    
    private CellActor createAndSetCell(int x, int y, Color color, float size) {
        CellActor cell = createCell(x, y, color, size);
        setCell(x, y, cell);
        return cell;
    }

    public CellActor createCell(int x, int y, Color color, float size) {
        CellActor cell = PooledPools.obtain(CellActor.class).initialize(skin, color, size);
        cell.setPosition(toStage(x), toStage(y));
        gridScene.addActor(cell);
        return cell;
    }

    private Color getRandomCellColor() {
        float value = GameApplication.Random.randomFloat();
        if (value < 0.333333f) return Color.Color0;
        if (value < 0.666666f) return Color.Color1;
        return Color.Color2;
    }

    public void setCell(int x, int y, CellActor cell) {
        cells[x][y] = cell;
        int colorNumber = cell.getCellColor().getNumber();
        if (colorNumber >= 0) {
            cellsByColor.get(colorNumber).add(cell);
        }
    }
    
    private float toStage(int grid) {
        return grid * paddedCellSize + offset + borderSize;
    }
    
    private int toGrid(float stage) {
        return (int) ((stage - borderSize) / paddedCellSize);
    }
    
    public int getColorCount(int colorNumber) {
        return cellsByColor.get(colorNumber).size;
    }

    public void select(Color color) {
        if (color == null) {
            deselectAll();
        } else {
            getSpriteDrawable(color).getSprite().setColor(skin.getColor(color.getName() + "-emphasize"));
        }
    }

    private void deselectAll() {
        for (int number = 0; number < Color.size(); number++) {
            getSpriteDrawable(Color.forNumber(number)).getSprite().setColor(skin.getColor(Color.forNumber(number).getName()));
        }
    }

    private SpriteDrawable getSpriteDrawable(Color color) {
        String colorName = color.getName();
        SpriteDrawable spriteDrawable = skin.optional(colorName, SpriteDrawable.class);
        if (spriteDrawable == null) {
            spriteDrawable = (SpriteDrawable) BackgroundBuilder.byColor(skin, colorName);
            skin.add(colorName, spriteDrawable, SpriteDrawable.class);
        }
        return spriteDrawable;
    }

    public void remove(Color color) {
        colorToRemove = color;
    }
    
    public int getGridWidth() {
        return cells.length;
    }
    
    public int getGridHeight() {
        return cells[0].length;
    }
    
}