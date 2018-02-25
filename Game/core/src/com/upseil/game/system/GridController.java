package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
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
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.util.RequiresResize;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialViewport;

public class GridController extends BaseSystem implements RequiresResize {
    
    private TagManager<Tag> tagManager;
    private ComponentMapper<GameState> gameStateMapper;
    private ComponentMapper<GridComponent> gridMapper;

    @Wire(name="Skin") private Skin skin;
    private SpriteDrawable[] coloredCellDrawables;
    private GridConfig config;
    
    private PaddedScreen screenDivider;
    private Stage stage;
    
    private Grid grid;
    private IntMap<Array<Image>> cellsByColor;
    
    private boolean updateScreenSize;
    private boolean initializeGrid;
    
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
        gridEdit.create(Scene.class).initialize(stage);
        gridEdit.create(InputHandler.class).setProcessor(stage);

        int expectedColorCount = getExpectedColorCount();
        coloredCellDrawables = new SpriteDrawable[Color.size()];
        cellsByColor = new IntMap<>();
        for (int number = 0; number < Color.size(); number++) {
            Sprite sprite = createSpriteForColor(number);
            coloredCellDrawables[number] = new SpriteDrawable(sprite);
            
            cellsByColor.put(number, new Array<>(expectedColorCount));
        }
        
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
    protected void processSystem() {
        if (updateScreenSize) { // FIXME This doesn't work when resizing
            int padding = config.getGridPadding();
            int top = Math.round(GameApplication.HUD.getTopHeight()) + padding;
            int left = Math.round(GameApplication.HUD.getLeftWidth()) + padding;
            int bottom = Math.round(GameApplication.HUD.getBottomHeight()) + padding;
            int right = Math.round(GameApplication.HUD.getRightWidth()) + padding;
            
            screenDivider.pad(top, left, bottom, right);
            updateScreenSize = false;
        }
        
        if (initializeGrid) {
            stage.clear();
            for (int number = 0; number < Color.size(); number++) {
                cellsByColor.get(number).clear();
            }
            
            grid = gridMapper.get(tagManager.getEntityId(Tag.Grid)).get();
            float cellSize = config.getCellSize();
            float paddedCellSize = cellSize + config.getSpacing();
            float offset = config.getSpacing() / 2;

            for (int x = 0; x < grid.getWidth(); x++) {
                for (int y = 0; y < grid.getHeight(); y++) {
                    Cell cell = grid.getCell(x, y);
                    Color cellColor = cell.getColor();
                    
                    Image cellImage = new Image(getDrawable(cellColor));
                    cellImage.setBounds(x * paddedCellSize + offset, y * paddedCellSize + offset, cellSize, cellSize);
                    stage.addActor(cellImage);
                    
                    int colorNumber = cellColor.getNumber();
                    if (colorNumber >= 0) {
                        cellsByColor.get(colorNumber).add(cellImage);
                    }
                }
            }
            
            initializeGrid = false;
        }
    }
    
    public Drawable getSyncedDrawable(int colorNumber) {
        if (colorNumber < 0 || colorNumber >= Color.size()) {
            throw new IndexOutOfBoundsException("Number must be between 0 and " + Color.size());
        }
        return coloredCellDrawables[colorNumber];
    }
    
    private Drawable getDrawable(Color color) {
        int number = color.getNumber();
        if (number >= 0) return coloredCellDrawables[number];
        return BackgroundBuilder.byColor(skin, color.getName());
    }
    
    public int getColorCount(int colorNumber) {
        return cellsByColor.get(colorNumber).size;
    }

    public void select(Color color) {
        if (color == null) {
            deselectAll();
        } else {
            coloredCellDrawables[color.getNumber()].getSprite().setColor(skin.getColor(color.getName() + "-emphasize"));
        }
    }

    private void deselectAll() {
        for (int number = 0; number < coloredCellDrawables.length; number++) {
            coloredCellDrawables[number].getSprite().setColor(skin.getColor(Color.forNumber(number).getName()));
        }
    }

    public void remove(Color color) {
        Array<Image> cells = cellsByColor.get(color.getNumber());
        for (Image cellImage : cells) {
            cellImage.remove();
        }
        
        gameStateMapper.get(tagManager.getEntityId(Tag.GameState)).incrementScore(cells.size);
        cells.clear();
    }

    @Override
    public void resize(int width, int height) {
        updateScreenSize = true;
    }
    
    @Override
    protected void dispose() {
        for (SpriteDrawable drawable : coloredCellDrawables) {
            drawable.getSprite().getTexture().dispose();
        }
    }
    
}
