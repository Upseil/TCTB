package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.upseil.game.GameApplication;
import com.upseil.game.GameConfig;
import com.upseil.game.Layers;
import com.upseil.game.Tag;
import com.upseil.game.component.GameState;
import com.upseil.game.scene2d.HUDStage;
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.viewport.PartialScreenViewport;
import com.upseil.gdx.viewport.ScreenDivider;
import com.upseil.gdx.viewport.ScreenRatioDivider;

public class GameInitializer extends BaseSystem {
    
    private TagManager<Tag> tagManager;
    private LayeredSceneRenderSystem<?> renderSystem;
    
    @Wire(name="Config") private GameConfig config;
    
    private boolean isProcessed;
    
    @Override
    protected void initialize() {
        GameState gameState = new GameState();
        gameState.setScore(0);
        
        Entity gameStateEntity = world.createEntity();
        gameStateEntity.edit().add(gameState);
        tagManager.register(Tag.GameState, gameStateEntity);
    }

    @Override
    protected boolean checkProcessing() {
        return !isProcessed;
    }

    @Override
    protected void processSystem() {
        initializeHUD();
        isProcessed = true;
    }

    private void initializeHUD() {
        ScreenDivider hudDivider = new ScreenRatioDivider("1:1");
        Viewport hudViewport = new PartialScreenViewport(hudDivider);
        GameApplication.HUD = new HUDStage(hudViewport, renderSystem.getGlobalBatch(), world);
        EntityEdit hud = world.createEntity().edit();
        hud.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        hud.create(Scene.class).initialize(GameApplication.HUD);
        hud.create(InputHandler.class).setProcessor(GameApplication.HUD);
    }
    
}
