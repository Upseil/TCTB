package com.upseil.game.system;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
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
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.util.Pair;
import com.upseil.gdx.util.RequiresResize;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialViewport;

public class GridController extends BaseSystem implements RequiresResize {
    
    private static final float MaxRemoveDelay = 0.5f;
    private static final float RemoveDuration = 1.0f;
    private static final float RemoveDistance = 150f;
    private static final float ActorMoveSpeed = 150f;
    
    private TagManager<Tag> tagManager;
    private ComponentMapper<GameState> gameStateMapper;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenDivider;
    private Stage stage;
    private float paddedCellSize;
    private float offset;
    
    private GameState gameState;
    
    private boolean updateScreenSize;
    private boolean initializeGrid;
    
    private CellActor[][] cells;
    private Array<ObjectSet<CellActor>> actorsByColor;
    private ObjectFloatMap<CellActor> actorRemovalDelays;
    private Array<Pair<CellActor, MoveToAction>> actorMovements;
    private Color colorToRemove;
    
    @Override
    protected void initialize() {
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getGridConfig();
        float worldSize = config.getGridSize() * (config.getCellSize() + config.getSpacing());
        
        screenDivider = new PaddedScreen();
        PartialViewport gridViewport = new PartialScalingViewport(screenDivider, Scaling.fit, worldSize, worldSize);
        stage = new Stage(gridViewport, world.getSystem(LayeredSceneRenderSystem.class).getGlobalBatch());
        
        Entity gridEntity = world.createEntity();
        EntityEdit gridEdit = gridEntity.edit();
        gridEdit.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        gridEdit.create(InputHandler.class).setProcessor(stage);
        Scene gridScene = gridEdit.create(Scene.class);
        gridScene.initialize(stage);//.setTimeScale(0.1f);
        tagManager.register(Tag.Grid, gridEntity);

        int gridSize = config.getGridSize();
        cells = new CellActor[gridSize][gridSize];
        int expectedColorCount = getExpectedColorCount();
        actorsByColor = new Array<>(true, Color.size(), ObjectSet.class);
        for (int number = 0; number < Color.size(); number++) {
            actorsByColor.add(new ObjectSet<>(expectedColorCount));
        }
        actorRemovalDelays = new ObjectFloatMap<>(expectedColorCount);
        actorMovements = new Array<>(true, config.getGridSize(), Pair.class);
        
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
        if (updateScreenSize) { // FIXME This doesn't work when resizing
            updateScreenSize();
            updateScreenSize = false;
        }
        
        if (initializeGrid) {
            stage.clear();
            for (int number = 0; number < Color.size(); number++) {
                actorsByColor.get(number).clear();
            }
            
            initializeGrid();
            initializeGrid = false;
        }
        
        if (actorRemovalDelays.size > 0) {
            processRemovalDelays();
        }
        if (colorToRemove != null) {
            removeCells();
            colorToRemove = null;
        }
    }

    public void processRemovalDelays() {
        Entries<CellActor> actorsToRemove = actorRemovalDelays.entries();
        while (actorsToRemove.hasNext()) {
            Entry<CellActor> entry = actorsToRemove.next();
            if (entry.value <= world.delta) {
                actorsToRemove.remove();
                gameState.incrementScore();
                
                Color cellColor = entry.key.getCellColor();
                ObjectSet<CellActor> actors = actorsByColor.get(cellColor.getNumber());
                actors.remove(entry.key);
                if (actors.size == 0) {
                    GameApplication.HUD.setUpdateValueLabels(true);
                    GameApplication.HUD.setContinousUpdate(false);
                    fillGaps(cellColor);
                }
            } else {
                actorRemovalDelays.put(entry.key, entry.value - world.delta);
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
            // First non-empty cell after a new position has been found -> Move actor
            if (cell != null && newX >= 0 && newY >= 0) {
                float stageX = toStage(newX);
                float stageY = toStage(newY);
                float duration = Math.max(Math.abs(cell.getX() - stageX), Math.abs(cell.getY() - stageY)) / ActorMoveSpeed;
                actorMovements.add(new Pair<CellActor, MoveToAction>(cell, moveTo(stageX, stageY, duration))); // TODO Pool pairs
                // This cell is now empty -> update new position
                cells[newX][newY] = cell;
                newX += colorRemoved == Color.Color1 ? 0 : colorRemoved == Color.Color0 ? 1 : -1;
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
                newX = -1;
                newY = -1;

                float delay = 0;
                Pair<CellActor, MoveToAction>[] movements = actorMovements.items;
                for (int index = actorMovements.size - 1; index >= 0; index--) {
                    CellActor actor = movements[index].getA();
                    MoveToAction moveAction = movements[index].getB();
                    if (index < actorMovements.size - 1) {
                        CellActor previousActor = movements[index + 1].getA();
                        float actorReference = colorRemoved == Color.Color1 ? actor.getY() + actor.getHeight() :
                                                                              actor.getX() + (colorRemoved == Color.Color0 ? actor.getWidth() : 0);
                        float previousActorReference = colorRemoved == Color.Color1 ? previousActor.getY() :
                                                                                      previousActor.getX() + (colorRemoved == Color.Color0 ? 0 : previousActor.getWidth());
                        delay += (Math.abs(actorReference - previousActorReference) - (offset * 2)) / ActorMoveSpeed;
                    }
                    actor.addAction(delay(delay, moveAction));
                }
                actorMovements.clear();
            }
        }
    }

    public void removeCells() {
        ExtendedRandom random = GameApplication.Random;
        ObjectSet<CellActor> actors = actorsByColor.get(colorToRemove.getNumber());
        for (CellActor actor : actors) {
            float removalDelay = random.randomFloat(0, MaxRemoveDelay);
            int x = toGrid(actor.getX());
            int y = toGrid(actor.getY());
            
            cells[x][y] = null;
            actorRemovalDelays.put(actor, removalDelay);

            actor.toFront();
            actor.addAction(sequence(delay(removalDelay),
                                     parallel(fadeOut(RemoveDuration, Interpolation.fade),
                                              scaleTo(0, 0, RemoveDuration, Interpolation.fade),
                                              moveBy(0, RemoveDistance, RemoveDuration, Interpolation.pow2In)),
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
        
        screenDivider.pad(top, left, bottom, right);
    }

    public void initializeGrid() {
        float cellSize = config.getCellSize();
        paddedCellSize = cellSize + config.getSpacing();
        offset = config.getSpacing() / 2;

        initializeBlackAndWhite(config.getExclusionAreaSize(), cellSize);
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                if (cells[x][y] == null) {
                    createCellActor(x, y, getRandomCellColor(), cellSize);
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
        int blackX = random.randomBoolean() ? random.randomInt(0, maxX - 1) : random.randomInt(minX, width - 1);
        int blackY = random.randomBoolean() ? random.randomInt(0, maxY - 1) : random.randomInt(minY, height - 1);
        int whiteX = width - blackX - 1;
        int whiteY = height - blackY - 1;

        createCellActor(blackX, blackY, Color.Black, cellSize);
        createCellActor(whiteX, whiteY, Color.White, cellSize);
    }
    
    private void createCellActor(int x, int y, Color color, float size) {
        CellActor actor = new CellActor(skin, color, size);
        actor.setPosition(toStage(x), toStage(y));
        stage.addActor(actor);
        
        cells[x][y] = actor;
        int colorNumber = color.getNumber();
        if (colorNumber >= 0) {
            actorsByColor.get(colorNumber).add(actor);
        }
    }
    
    private float toStage(int grid) {
        return grid * paddedCellSize + offset;
    }
    
    private int toGrid(float stage) {
        return (int) (stage / paddedCellSize);
    }
    
    public int getColorCount(int colorNumber) {
        return actorsByColor.get(colorNumber).size;
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

    @Override
    public void resize(int width, int height) {
        updateScreenSize = true;
    }
    
}