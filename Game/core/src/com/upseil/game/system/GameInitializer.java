package com.upseil.game.system;

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
import com.upseil.gdx.artemis.component.Screen;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.PassiveSystem;
import com.upseil.gdx.artemis.system.ScreenManager;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.viewport.PartialScreenViewport;
import com.upseil.gdx.viewport.ScreenDivider;
import com.upseil.gdx.viewport.ScreenRatioDivider;

public class GameInitializer extends PassiveSystem {
    
    private TagManager<Tag> tagManager;
    private ScreenManager screenManager;
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
        initializeGameMenu();
    }

    private void initializeGameMenu() {
        Viewport menuViewport = new ScreenViewport();
        MenuStage menuStage = new MenuStage(menuViewport, renderSystem.getGlobalBatch(), world);
        EntityEdit menuStageEntity = world.createEntity().edit();
        menuStageEntity.create(Layer.class).setZIndex(Layers.UI.getZIndex());
        menuStageEntity.create(Scene.class).initialize(menuStage);
        menuStageEntity.create(InputHandler.class).setProcessor(menuStage);
        tagManager.register(Tag.Menu, menuStageEntity.getEntityId());
        
        EntityEdit menuScreenEntity = world.createEntity().edit();
        Screen menuScreen = menuScreenEntity.create(Screen.class);
        menuScreen.addScene(menuStageEntity.getEntityId());
//        menuScreen.setEntranceAction(menuStage.getEntranceAction());
        menuScreen.setExitAction(menuStage.getExitAction());
        tagManager.register(Tag.MenuScreen, menuScreenEntity.getEntityId());
        
        screenManager.setScreen(menuScreenEntity.getEntityId(), false);
    }

    private void initializeHUD() {
        ScreenDivider hudDivider = new ScreenRatioDivider("1:1");
        Viewport hudViewport = new PartialScreenViewport(hudDivider);
        HUDStage hudStage = new HUDStage(hudViewport, renderSystem.getGlobalBatch(), world);
        EntityEdit hudStageEntity = world.createEntity().edit();
        hudStageEntity.create(Layer.class).setZIndex(Layers.HUD.getZIndex());
        hudStageEntity.create(Scene.class).initialize(hudStage);
        hudStageEntity.create(InputHandler.class).setProcessor(hudStage);
        hudStageEntity.create(Ignore.class);
        tagManager.register(Tag.HUD, hudStageEntity.getEntityId());
        
        EntityEdit gameScreenEntity = world.createEntity().edit();
        Screen gameScreen = gameScreenEntity.create(Screen.class);
        gameScreen.addScene(hudStageEntity.getEntityId());
        tagManager.register(Tag.GameScreen, gameScreenEntity.getEntityId());
    }
    
}
