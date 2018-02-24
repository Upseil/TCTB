package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.scenes.scene2d.Stage;
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

public class GameInitializer extends BaseSystem {
    
    private TagManager<Tag> tagManager;
    private LayeredSceneRenderSystem<?> renderSystem;
    
    @Wire(name="Config") private GameConfig config;
    
    @Override
    protected void initialize() {
        GameState gameState = new GameState();
        gameState.setScore(150);
        
        Entity gameStateEntity = world.createEntity();
        gameStateEntity.edit().add(gameState);
        tagManager.register(Tag.GameState, gameStateEntity);
        
        initializeHUD();
    }

    private void initializeHUD() {
        Stage uiStage = new HUDStage(renderSystem.getGlobalBatch(), world);
        EntityEdit hud = world.createEntity().edit();
        hud.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        hud.create(Scene.class).initialize(uiStage);
        hud.create(InputHandler.class).setProcessor(uiStage);
    }

    @Override
    protected boolean checkProcessing() {
        return false;
    }

    @Override
    protected void processSystem() { }
    
}
