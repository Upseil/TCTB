package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.upseil.game.Config.GameConfig;
import com.upseil.game.Constants.Layers;
import com.upseil.game.Constants.Tag;
import com.upseil.game.component.GameState;
import com.upseil.game.scene2d.HUDStage;
import com.upseil.game.scene2d.MenuStage;
import com.upseil.gdx.artemis.component.Ignore;
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
        initializeGameMenu();
        initializeHUD();
        isProcessed = true;
    }

    private void initializeGameMenu() {
        Viewport menuViewport = new ScreenViewport();
        MenuStage menuStage = new MenuStage(menuViewport, renderSystem.getGlobalBatch(), world);
        EntityEdit menu = world.createEntity().edit();
        menu.create(Layer.class).setZIndex(Layers.UI.getZIndex());
        menu.create(Scene.class).initialize(menuStage);
        menu.create(InputHandler.class).setProcessor(menuStage);
        tagManager.register(Tag.Menu, menu.getEntityId());
    }

    private void initializeHUD() {
        ScreenDivider hudDivider = new ScreenRatioDivider("1:1");
        Viewport hudViewport = new PartialScreenViewport(hudDivider);
        HUDStage hudStage = new HUDStage(hudViewport, renderSystem.getGlobalBatch(), world);
        EntityEdit hud = world.createEntity().edit();
        hud.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        hud.create(Scene.class).initialize(hudStage);
        hud.create(InputHandler.class).setProcessor(hudStage);
        hud.create(Ignore.class);
        tagManager.register(Tag.HUD, hud.getEntityId());
    }
    
}
