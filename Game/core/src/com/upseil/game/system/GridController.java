package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.EntityEdit;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import com.upseil.game.GameApplication;
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.GridConfig;
import com.upseil.game.Layers;
import com.upseil.game.Tag;
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.util.RequiresResize;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialViewport;

public class GridController extends BaseSystem implements RequiresResize {
    
    private TagManager<Tag> tagManager;
//    private ComponentMapper<GridComponent> gridMapper;
    
    private GridConfig config;
    
    private PaddedScreen screenDivider;
    
    private boolean updateScreen;
    
    @Override
    protected void initialize() {
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getGridConfig();
        float worldSize = config.getGridSize() * config.getCellSize() + (config.getGridSize() - 1) * config.getSpacing();
        
        screenDivider = new PaddedScreen();
        PartialViewport gridViewport = new PartialScalingViewport(screenDivider, Scaling.fit, worldSize, worldSize);
        Stage gridStage = new Stage(gridViewport, world.getSystem(LayeredSceneRenderSystem.class).getGlobalBatch());
        
        EntityEdit gridEdit = tagManager.getEntity(Tag.Grid).edit();
        gridEdit.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        gridEdit.create(Scene.class).initialize(gridStage);
        gridEdit.create(InputHandler.class).setProcessor(gridStage);
        
        Skin skin = world.getRegistered("Skin");
        Image dummy = new Image(skin, "t-dot");
        dummy.setBounds(0, 0, worldSize, worldSize);
        gridStage.addActor(dummy);
    }
    
    @Override
    protected void processSystem() {
        if (updateScreen) { // FIXME This doesn't work when resizing
            int padding = config.getGridPadding();
            int top = Math.round(GameApplication.HUD.getTopHeight()) + padding;
            int left = Math.round(GameApplication.HUD.getLeftWidth()) + padding;
            int bottom = Math.round(GameApplication.HUD.getBottomHeight()) + padding;
            int right = Math.round(GameApplication.HUD.getRightWidth()) + padding;
            
            screenDivider.pad(top, left, bottom, right);
            updateScreen = false;
        }
    }

    @Override
    public void resize(int width, int height) {
        updateScreen = true;
    }
    
}
