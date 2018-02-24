package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.upseil.game.GameApplication;
import com.upseil.game.GameConfig;
import com.upseil.game.Layers;
import com.upseil.game.Tag;
import com.upseil.game.component.GameState;
import com.upseil.game.component.GridComponent;
import com.upseil.game.domain.Grid;
import com.upseil.game.scene2d.HUDStage;
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;

public class GameInitializer extends BaseSystem {
    
    private TagManager<Tag> tagManager;
    private LayeredSceneRenderSystem<?> renderSystem;
    
    @Wire(name="Config") private GameConfig config;
    
    @Override
    protected void initialize() {
        GameState gameState = new GameState();
        gameState.setScore(0);
        
        Entity gameStateEntity = world.createEntity();
        gameStateEntity.edit().add(gameState);
        tagManager.register(Tag.GameState, gameStateEntity);
        
        initializeHUD();
        initializeGrid();
    }

    private void initializeHUD() {
        GameApplication.HUD = new HUDStage(renderSystem.getGlobalBatch(), world);
        EntityEdit hud = world.createEntity().edit();
        hud.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        hud.create(Scene.class).initialize(GameApplication.HUD);
        hud.create(InputHandler.class).setProcessor(GameApplication.HUD);
    }

    private void initializeGrid() {
        int gridSize = config.getGridConfig().getGridSize();
        Entity gridEntity = world.createEntity();
        gridEntity.edit().add(new GridComponent().set(new Grid(gridSize, gridSize, config.getGridConfig().getExclusionAreaSize())));
        tagManager.register(Tag.Grid, gridEntity);
    }

    @Override
    protected boolean checkProcessing() {
        return false;
    }

    @Override
    protected void processSystem() { }
    
}
