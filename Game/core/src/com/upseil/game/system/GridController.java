package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import com.upseil.game.GameApplication;
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.GridConfig;
import com.upseil.game.Layers;
import com.upseil.game.Tag;
import com.upseil.game.component.GridComponent;
import com.upseil.game.domain.Cell;
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
    private ComponentMapper<GridComponent> gridMapper;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenDivider;
    private Stage stage;
    
    private Grid grid;
    
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
        
        initializeGrid = true;
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
            grid = gridMapper.get(tagManager.getEntityId(Tag.Grid)).get();
            float cellSize = config.getCellSize();
            float paddedCellSize = cellSize + config.getSpacing();
            float offset = config.getSpacing() / 2;

            BackgroundBuilder imageBuilder = BackgroundBuilder.get(skin);
            for (int x = 0; x < grid.getWidth(); x++) {
                for (int y = 0; y < grid.getHeight(); y++) {
                    Cell cell = grid.getCell(x, y);
                    Image cellImage = new Image(imageBuilder.color(cell.getColor().getName()).build());
                    cellImage.setBounds(x * paddedCellSize + offset, y * paddedCellSize + offset, cellSize, cellSize);
                    stage.addActor(cellImage);
                }
            }
            
            initializeGrid = false;
        }
    }

    @Override
    public void resize(int width, int height) {
        updateScreenSize = true;
    }
    
}
