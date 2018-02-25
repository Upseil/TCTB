package com.upseil.game.system;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.IntMap;
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
import com.upseil.game.component.GridComponent;
import com.upseil.game.domain.Cell;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Grid;
import com.upseil.game.scene2d.CellActor;
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.util.RequiresResize;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialViewport;

public class GridController extends BaseSystem implements RequiresResize {
    
    private static final float MaxRemoveDelay = 0.5f;
    private static final float RemoveDuration = 1.0f;
    private static final float RemoveDistance = 150f;
    
    private TagManager<Tag> tagManager;
    private ComponentMapper<GameState> gameStateMapper;
    private ComponentMapper<GridComponent> gridMapper;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenDivider;
    private Stage stage;
    private float paddedCellSize;
    private float offset;
    
    private GameState gameState;
    private Grid grid;
    
    private boolean updateScreenSize;
    private boolean initializeGrid;
    
    private IntMap<ObjectSet<CellActor>> cellsByColor;
    private ObjectFloatMap<CellActor> cellRemovalDelays;
    private Color colorToRemove;
    
    @Override
    protected void initialize() {
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getGridConfig();
        float worldSize = config.getGridSize() * (config.getCellSize() + config.getSpacing());
        
        screenDivider = new PaddedScreen();
        PartialViewport gridViewport = new PartialScalingViewport(screenDivider, Scaling.fit, worldSize, worldSize);
        stage = new Stage(gridViewport, world.getSystem(LayeredSceneRenderSystem.class).getGlobalBatch());
        
        EntityEdit gridEdit = tagManager.getEntity(Tag.Grid).edit();
        gridEdit.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        gridEdit.create(InputHandler.class).setProcessor(stage);
        Scene gridScene = gridEdit.create(Scene.class);
        gridScene.initialize(stage);//.setTimeScale(0.1f);

        int expectedColorCount = getExpectedColorCount();
        cellsByColor = new IntMap<>();
        for (int number = 0; number < Color.size(); number++) {
            cellsByColor.put(number, new ObjectSet<>(expectedColorCount));
        }
        cellRemovalDelays = new ObjectFloatMap<>(expectedColorCount);
        
        initializeGrid = true;
    }

    public int getExpectedColorCount() {
        Grid grid = gridMapper.get(tagManager.getEntityId(Tag.Grid)).get();
        int expectedColorCount = (grid.getWidth() * grid.getHeight()) / Color.size();
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
        grid = gridMapper.get(tagManager.getEntityId(Tag.Grid)).get();
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
                cellsByColor.get(number).clear();
            }
            
            initializeGrid();
            initializeGrid = false;
        }
        
        if (cellRemovalDelays.size > 0) {
            Entries<CellActor> cells = cellRemovalDelays.entries();
            while (cells.hasNext()) {
                Entry<CellActor> entry = cells.next();
                if (entry.value <= world.delta) {
                    removeCell(entry.key);
                    cells.remove();
                } else {
                    cellRemovalDelays.put(entry.key, entry.value - world.delta);
                }
            }
        }
        
        if (colorToRemove != null) {
            ExtendedRandom random = GameApplication.Random;
            ObjectSet<CellActor> cells = cellsByColor.get(colorToRemove.getNumber());
            for (CellActor cell : cells) {
                float removalDelay = random.randomFloat(0, MaxRemoveDelay);
                grid.removeCell(toGrid(cell.getX()), toGrid(cell.getY()));
                cellRemovalDelays.put(cell, removalDelay);

                cell.toFront();
                cell.addAction(sequence(delay(removalDelay),
                                        parallel(fadeOut(RemoveDuration, Interpolation.fade),
                                                 scaleTo(0, 0, RemoveDuration, Interpolation.fade),
                                                 moveBy(0, RemoveDistance, RemoveDuration, Interpolation.pow2In)),
                                        removeActor()));
            }
            GameApplication.HUD.setContinousUpdate(true);
            colorToRemove = null;
        }
    }

    private void removeCell(CellActor cell) {
        gameState.incrementScore();
        
        ObjectSet<CellActor> cells = cellsByColor.get(cell.getCellColor().getNumber());
        cells.remove(cell);
        if (cells.size == 0) {
            GameApplication.HUD.setUpdateValueLabels(true);
            GameApplication.HUD.setContinousUpdate(false);
        }
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

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                Color cellColor = cell.getColor();
                
                CellActor cellActor = new CellActor(skin, cellColor, cellSize);
                cellActor.setPosition(toStage(x), toStage(y));
                stage.addActor(cellActor);
                
                int colorNumber = cellColor.getNumber();
                if (colorNumber >= 0) {
                    cellsByColor.get(colorNumber).add(cellActor);
                }
            }
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

    @Override
    public void resize(int width, int height) {
        updateScreenSize = true;
    }
    
}