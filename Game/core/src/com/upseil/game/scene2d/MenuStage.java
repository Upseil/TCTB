package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.upseil.game.Config.MenuConfigValues.ControlsFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.ControlsFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.ControlsTopPadding;
import static com.upseil.game.Config.MenuConfigValues.GlassAlpha;
import static com.upseil.game.Config.MenuConfigValues.GridSize;
import static com.upseil.game.Config.MenuConfigValues.LogoAnimation;
import static com.upseil.game.Config.MenuConfigValues.LogoHorizontalPadding;
import static com.upseil.game.Config.MenuConfigValues.LogoMoveDuration;
import static com.upseil.game.Config.MenuConfigValues.LogoTopPadding;

import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.upseil.game.Config.GameConfig;
import com.upseil.game.Config.MenuConfig;
import com.upseil.game.Constants.Tag;
import com.upseil.game.GameApplication;
import com.upseil.game.scene2d.MenuGridBackground.MenuGridBackgroundStyle;
import com.upseil.game.system.GridController;
import com.upseil.gdx.action.Action;
import com.upseil.gdx.artemis.component.Screen;
import com.upseil.gdx.artemis.system.ScreenManager;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.SimpleChangeListener;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.scene2d.util.SimpleGenericValue;

@Wire
public class MenuStage extends Stage {
    
    private TagManager<Tag> tagManager;
    private ScreenManager screenManager;
    
    @Wire(name="Skin") private Skin skin;
    @Wire(name="UI") private I18NBundle hudMessages;
    
    private final World world;
    private final MenuStyle style;
    
    private final Actor background;
    private final MenuGridBackground grid;
    private final Actor glass;
    private final LogoGroup logo;
    
    private float width;
    private float height;
    
    public MenuStage(Viewport viewport, Batch batch, World world) {
        super(viewport, batch);
        world.inject(this);
        this.world = world;
        
        GameConfig gameConfig = world.getRegistered("Config");
        MenuConfig config = gameConfig.getMenuConfig();
        style = new MenuStyle(config);
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        
        logo = new LogoGroup(worldWidth, LogoGroup.AnimationStyle.from(config));
        
        Button startGameButton = new TextButton(hudMessages.get("startGame"), skin, "menu2");
        startGameButton.addListener(new SimpleChangeListener(this::startGame));
        
        Button exitButton = null;
        ApplicationType applicationType = Gdx.app.getType();
        if (applicationType == ApplicationType.Android || applicationType == ApplicationType.Desktop) {
            exitButton = new TextButton(hudMessages.get("exit"), skin, "menu1");
            exitButton.addListener(new SimpleChangeListener(() -> Gdx.app.exit()));
        }
        
        // TODO Bigger buttons
        Table controls = new Table(skin);
        controls.setFillParent(true);
        controls.top().padTop(new SimpleGenericValue(() -> style.logoTopPadding + logo.getPrefHeight() + style.controlsTopPadding));
        
        controls.defaults().space(25).fillX();
        controls.add(startGameButton);
        if (exitButton != null) {
            controls.row();
            controls.add(exitButton);
        }
        
        Color screenBackgroundColor = skin.getColor("t-screen-background");
        float glassAlpha = config.getFloat(GlassAlpha);
        float inverseGlassAlpha = 1 / (1 - glassAlpha);
        Color backgroundColor = screenBackgroundColor.cpy().mul(inverseGlassAlpha);
        
        background = new Image(BackgroundBuilder.byColor(skin, backgroundColor));
        grid = new MenuGridBackground(world, new MenuGridBackgroundStyle(config), GameApplication.Random, config.getInt(GridSize));
        glass = new Image(BackgroundBuilder.byColor(skin, "black", glassAlpha));

        addActor(background);
        addActor(grid);
        addActor(glass);
        addActor(controls);
        addActor(logo);
        
        sizeChanged(worldWidth, worldHeight);
        width = worldWidth;
        height = worldHeight;

        if (config.getBoolean(LogoAnimation)) {
            float logoAnimationDuration = logo.getAnimationDuration();
            
            logo.setPosition((worldWidth - logo.getPrefWidth()) / 2, (worldHeight - logo.getPrefHeight()) / 2);
            logo.addAction(
                delay(
                    logoAnimationDuration,
                    moveTo(logo.getX(), getHeight() - logo.getPrefHeight() - style.logoTopPadding, style.logoMoveDuration, Interpolation.swing)
                )
            );
            
            controls.getColor().a = 0;
            controls.addAction(delay(logoAnimationDuration + style.controlsFadeInDelay, fadeIn(style.controlsFadeInDuration, Interpolation.fade)));

            grid.randomEntrance(logoAnimationDuration + style.controlsFadeInDelay + style.controlsFadeInDuration);
        }
    }
    
    private void startGame() {
        screenManager.setScreen(tagManager.getEntityId(Tag.GameScreen));
    }

    public Action<Screen, ?> getEntranceAction() {
        // TODO
        return null;
    }

    public Action<Screen, ?> getExitAction() {
        return Action.Unsafe(deltaTime -> {
            world.getSystem(GridController.class).onScreenSizeChanged();
            return true;
        });
    }
    
    @Override
    public void draw() {
        float worldWidth = getViewport().getWorldWidth();
        float worldHeight = getViewport().getWorldHeight();
        if (width != worldWidth || height != worldHeight) {
            sizeChanged(worldWidth, worldHeight);
            width = worldWidth;
            height = worldHeight;
        }
        
        super.draw();
    }

    private void sizeChanged(float newWidth, float newHeight) {
        background.setSize(newWidth, newHeight);
        glass.setSize(newWidth, newHeight);
        grid.setScale(newWidth / grid.getWorldWidth(), newHeight / grid.getWorldHeight());

        if (logo.getActions().size <= 0) {
            float paddedLogoMaxWidth = logo.getMaxWidth() + 2 * style.logoHorizontalPadding;
            logo.setScale(Math.min(newWidth / paddedLogoMaxWidth, 1));
            
            float newLogoX = (newWidth - logo.getPrefWidth()) / 2;
            float newLogoY = newHeight - logo.getPrefHeight() - style.logoTopPadding;
            logo.setPosition(newLogoX, newLogoY);
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        logo.dispose();
    }
    
    private static class MenuStyle {
        
        public final float logoTopPadding;
        public final float logoHorizontalPadding;
        public final float logoMoveDuration;
        public final float controlsTopPadding;
        public final float controlsFadeInDelay;
        public final float controlsFadeInDuration;
        
        public MenuStyle(MenuConfig config) {
            this(config.getFloat(LogoTopPadding), config.getFloat(LogoHorizontalPadding), config.getFloat(LogoMoveDuration), config.getFloat(ControlsTopPadding),
                    config.getFloat(ControlsFadeInDelay), config.getFloat(ControlsFadeInDuration));
        }
        
        public MenuStyle(float logoTopPadding, float logoHorizontalPadding, float logoMoveDuration, float controlsTopPadding, float controlsFadeInDelay,
                float controlsFadeInDuration) {
            this.logoTopPadding = logoTopPadding;
            this.logoHorizontalPadding = logoHorizontalPadding;
            this.logoMoveDuration = logoMoveDuration;
            this.controlsTopPadding = controlsTopPadding;
            this.controlsFadeInDelay = controlsFadeInDelay;
            this.controlsFadeInDuration = controlsFadeInDuration;
        }
        
    }
    
}