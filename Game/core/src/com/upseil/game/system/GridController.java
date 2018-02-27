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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
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
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.util.Pair;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialWorldViewport;

public class GridController extends BaseSystem {
    
    private TagManager<Tag> tagManager;
    private ComponentMapper<GameState> gameStateMapper;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenPadding;
    private Scene gridScene;
    private float cellSize;
    private float paddedCellSize;
    private float offset;
    
    private GameState gameState;
    
    private boolean updateScreenSize;
    private boolean initializeGrid;
    
    private CellActor[][] cells;
    private Array<ObjectSet<CellActor>> cellsByColor;
    
    private ObjectFloatMap<CellActor> cellRemovalDelays;
    private Color colorToRemove;
    private float maxRemovalDelay;
    private float removalDuration;
    private float removalMoveAmount;
    private float removalScaleTo;
    
    private Array<Pair<CellActor, MoveToAction>> cellMovements;
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
        
        float worldSize = config.getGridSize() * (config.getCellSize() + config.getSpacing());
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
        cells = new CellActor[gridSize][gridSize];
        int expectedColorCount = getExpectedColorCount();
        cellsByColor = new Array<>(true, Color.size(), ObjectSet.class);
        for (int number = 0; number < Color.size(); number++) {
            cellsByColor.add(new ObjectSet<>(expectedColorCount));
        }
        cellRemovalDelays = new ObjectFloatMap<>(expectedColorCount);
        cellMovements = new Array<>(true, config.getGridSize(), Pair.class);
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
            for (int number = 0; number < Color.size(); number++) {
                cellsByColor.get(number).clear();
            }
            
            initializeGrid();
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
        }
    }

    public void processNewCells() {
        Iterator<CellActor> newCellsIterator = newCells.iterator();
        while (newCellsIterator.hasNext()) {
            CellActor cell = newCellsIterator.next();
            float cellMaxX = cell.getX() + cell.getWidth();
            float cellMaxY = cell.getY() + cell.getHeight();
            if (cell.getX() >= 0 && cell.getY() >= 0 && cellMaxX <= gridScene.getWidth() && cellMaxY <= gridScene.getHeight()) {
                cellsByColor.get(cell.getCellColor().getNumber()).add(cell);
                newCellsIterator.remove();
            }
        }
        
        if (newCells.size == 0) {
            GameApplication.HUD.setUpdateValueLabels(true);
            GameApplication.HUD.setContinousUpdate(false);
            GameApplication.HUD.setButtonsDisabled(false);
        }
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
                cellMovements.add(new Pair<CellActor, MoveToAction>(cell, moveTo(stageX, stageY, duration))); // TODO Pool pairs
                // This cell is now empty -> update new position
                cells[newX][newY] = cell;
                newX += colorRemoved == Color.Color1 ? 0 :
                        colorRemoved == Color.Color0 ? 1 : -1;
                newY += colorRemoved == Color.Color1 ? 1 : 0;
            }
            
            // Advance or break
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
                if (newX >= 0 && newY >= 0) {
                    int newCellsCount = colorRemoved == Color.Color0 ? getGridWidth() - newX :
                                        colorRemoved == Color.Color1 ? getGridHeight() - newY : newX + 1;
                    for (int i = 0; i < newCellsCount; i++) {
                        int spawnX = colorRemoved == Color.Color0 ? newX + newCellsCount :
                                     colorRemoved == Color.Color1 ? newX: newX - newCellsCount;
                        int spawnY = colorRemoved == Color.Color1 ? newY + newCellsCount : newY;
                        
                        CellActor newCell = createCell(spawnX, spawnY, getRandomCellColor(), cellSize);
                        cells[newX][newY] = newCell;
                        newCells.add(newCell);
                        
                        float stageX = toStage(newX);
                        float stageY = toStage(newY);
                        float duration = Math.max(Math.abs(newCell.getX() - stageX), Math.abs(newCell.getY() - stageY)) / cellMoveSpeed;
                        cellMovements.add(new Pair<CellActor, MoveToAction>(newCell, moveTo(stageX, stageY, duration)));
    
                        newX += colorRemoved == Color.Color1 ? 0 :
                                colorRemoved == Color.Color0 ? 1 : -1;
                        newY += colorRemoved == Color.Color1 ? 1 : 0;
                    }
                    
                    float delay = 0;
                    Pair<CellActor, MoveToAction>[] movements = cellMovements.items;
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
                    cellMovements.clear();
                }
                
                newX = -1;
                newY = -1;
            }
        }
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
        cellSize = config.getCellSize();
        paddedCellSize = cellSize + config.getSpacing();
        offset = config.getSpacing() / 2;

        initializeBlackAndWhite(config.getExclusionAreaSize(), cellSize);
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                if (cells[x][y] == null) {
                    createAndSetCell(x, y, getRandomCellColor(), cellSize);
                }
            }
        }
    }

    private Color getRandomCellColor() {
        float value = GameApplication.Random.randomFloat();
        if (value < 0.333333f) return Color.Color0;
        if (value < 0.666666f) return Color.Color1;
        return Color.Color2;
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

        createAndSetCell(blackX, blackY, Color.Black, cellSize);
        createAndSetCell(whiteX, whiteY, Color.White, cellSize);
    }
    
    private void createAndSetCell(int x, int y, Color color, float size) {
        setCell(x, y, createCell(x, y, color, size));
    }

    public CellActor createCell(int x, int y, Color color, float size) {
        CellActor cell = new CellActor(skin, color, size);
        cell.setPosition(toStage(x), toStage(y));
        gridScene.addActor(cell);
        return cell;
    }

    public void setCell(int x, int y, CellActor cell) {
        cells[x][y] = cell;
        int colorNumber = cell.getCellColor().getNumber();
        if (colorNumber >= 0) {
            cellsByColor.get(colorNumber).add(cell);
        }
    }
    
    private float toStage(int grid) {
        return grid * paddedCellSize + offset;
    }
    
    private int toGrid(float stage) {
        return (int) (stage / paddedCellSize);
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