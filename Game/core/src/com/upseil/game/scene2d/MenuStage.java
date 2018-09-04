package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.upseil.game.Config.MenuConfigValues.GlassAlpha;
import static com.upseil.game.Config.MenuConfigValues.GridSize;
import static com.upseil.game.Config.MenuConfigValues.TitleAnimation;

import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.upseil.game.Config.GameConfig;
import com.upseil.game.Config.MenuConfig;
import com.upseil.game.Constants.Tag;
import com.upseil.game.GameApplication;
import com.upseil.game.math.Swing2Out;
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
    
    private static final float TitleTopPadding = 50;
    private static final float TitleMoveDuration = 1f;
    
    private static final float ControlsTopPadding = 100;
    private static final float ControlsFadeInDelay = TitleMoveDuration / 2;
    private static final float ControlsFadeInDuration = 1f;

    private TagManager<Tag> tagManager;
    private ScreenManager screenManager;
    
    @Wire(name="Skin") private Skin skin;
    @Wire(name="UI") private I18NBundle hudMessages;
    
    private final World world;
    
    private final Actor background;
    private final MenuGridBackground grid;
    private final Actor glass;
    private final TitleGroup title;
    
    private float width;
    private float height;
    
    public MenuStage(Viewport viewport, Batch batch, World world) {
        super(viewport, batch);
        world.inject(this);
        this.world = world;
        
        GameConfig gameConfig = world.getRegistered("Config");
        MenuConfig config = gameConfig.getMenuConfig();
        boolean titleAnimation = config.getBoolean(TitleAnimation);
        
        width = viewport.getWorldWidth();
        height = viewport.getWorldHeight();
        
        title = new TitleGroup(width, titleAnimation);
        
        Button startGameButton = new TextButton(hudMessages.get("startGame"), skin, "menu2");
        startGameButton.addListener(new SimpleChangeListener(this::startGame));
        
        Button exitButton = null;
        ApplicationType applicationType = Gdx.app.getType();
        if (applicationType == ApplicationType.Android || applicationType == ApplicationType.Desktop) {
            exitButton = new TextButton(hudMessages.get("exit"), skin, "menu1");
            exitButton.addListener(new SimpleChangeListener(() -> Gdx.app.exit()));
        }
        
        Table controls = new Table(skin);
        controls.setFillParent(true);
        controls.top().padTop(new SimpleGenericValue(() -> TitleTopPadding + title.getPrefHeight() + ControlsTopPadding));
        
        controls.defaults().space(25).fillX();
        controls.add(startGameButton);
        if (exitButton != null) {
            controls.row();
            controls.add(exitButton);
        }
        
        background = new Image(BackgroundBuilder.byColor(skin, "white"));
        background.setSize(width, height);
        MenuGridBackgroundStyle gridStyle = new MenuGridBackgroundStyle(config);
        grid = new MenuGridBackground(world, gridStyle, GameApplication.Random, config.getInt(GridSize));
        grid.setScale(width / grid.getWorldWidth(), height / grid.getWorldHeight());
        glass = new Image(BackgroundBuilder.byColor(skin, "black", config.getFloat(GlassAlpha)));
        glass.setSize(width, height);

        addActor(background);
        addActor(grid);
        addActor(glass);
        addActor(controls);
        addActor(title);

        if (titleAnimation) {
            title.setPosition((width - title.getPrefWidth()) / 2, (height - title.getPrefHeight()) / 2);
            title.addAction(delay(title.getAnimationDuration(),
                    moveTo(title.getX(), getHeight() - title.getPrefHeight() - TitleTopPadding, TitleMoveDuration, Interpolation.swing)));
            
            controls.getColor().a = 0;
            controls.addAction(delay(title.getAnimationDuration() + ControlsFadeInDelay, fadeIn(ControlsFadeInDuration, Interpolation.fade)));

            grid.randomEntrance(title.getAnimationDuration() + ControlsFadeInDelay + ControlsFadeInDuration);
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
        title.invalidate();
    }
    
    private static class TitleGroup extends WidgetGroup {
        
        private static final float FlyInDelay = 0.5f;
        private static final float FlyInDuration = 1.5f;
        private static final Interpolation FlyInInterpolation = new Swing2Out();
        
        private static final float OutlinesFadeInDelay = FlyInDelay + FlyInDuration + 0.1f;
        private static final float OutlinesFadeInDuration = 1f;
        
        private static final float FillingsFadeInDelay = OutlinesFadeInDelay + OutlinesFadeInDuration * 0.5f;
        private static final float FillingsFadeInDuration = 0.4f;
        private static final float MaxAdditionalFillingsFadeInDelay = 1f;

        private final int width;
        private final int height;
        
        private float highestAdditionalFillingsFadeInDelay;
        
        public TitleGroup(float worldWidth, boolean titleAnimation) {
            TextureAtlas titleAtlas = new TextureAtlas("title/title.atlas");
            
            AtlasRegion backgroundWhiteRegion = titleAtlas.findRegion("background-white");
            AtlasRegion backgroundBlackRegion = titleAtlas.findRegion("background-black");
            this.width = backgroundWhiteRegion.originalWidth;
            this.height = backgroundWhiteRegion.originalHeight;
            
            if (this.width > worldWidth) {
                this.setScale((worldWidth - 2 * TitleTopPadding) / this.width);
            }
            
            float slope = (float) height / (backgroundWhiteRegion.packedWidth + backgroundBlackRegion.packedWidth - width);

            Image backgroundWhite = createAlignedImage(backgroundWhiteRegion);
            addActor(backgroundWhite);
            Image backgroundBlack = createAlignedImage(backgroundBlackRegion);
            addActor(backgroundBlack);
            
            if (titleAnimation) {
                float backgroundWhiteStartX = worldWidth * -1.0f;
                float backgroundWhiteStartY = slope * backgroundWhiteStartX;
                backgroundWhite.setPosition(backgroundWhiteStartX, backgroundWhiteStartY);
                backgroundWhite.addAction(
                        delay(FlyInDelay, moveTo(backgroundWhiteRegion.offsetX, backgroundWhiteRegion.offsetY, FlyInDuration, FlyInInterpolation)));
                
                float backgroundBlackStartX = backgroundWhiteStartX * -1 + backgroundBlackRegion.offsetX;
                float backgroundBlackStartY = -1 * backgroundWhiteStartY;
                backgroundBlack.setPosition(backgroundBlackStartX, backgroundBlackStartY);
                backgroundBlack.addAction(
                        delay(FlyInDelay, moveTo(backgroundBlackRegion.offsetX, backgroundBlackRegion.offsetY, FlyInDuration, FlyInInterpolation)));
            }
            
            for (AtlasRegion region : titleAtlas.getRegions()) {
                if (region.name.startsWith("filling-")) {
                    if (titleAnimation) {
                        float additionalDelay = (region.offsetX / width) * MaxAdditionalFillingsFadeInDelay;
                        if (additionalDelay > highestAdditionalFillingsFadeInDelay) {
                            highestAdditionalFillingsFadeInDelay = additionalDelay;
                        }
                        float totalDelay = FillingsFadeInDelay + additionalDelay;
                        addActor(createFadeInImage(region, totalDelay, FlyInDuration));
                    } else {
                        addActor(createAlignedImage(region));
                    }
                }
            }

            AtlasRegion outlinesStayRegion = titleAtlas.findRegion("outlines-stay");
            AtlasRegion outlinesColorfullRegion = titleAtlas.findRegion("outlines-colorful");
            if (titleAnimation) {
                addActor(createFadeInImage(outlinesStayRegion, OutlinesFadeInDelay, OutlinesFadeInDuration));
                addActor(createFadeInImage(outlinesColorfullRegion, OutlinesFadeInDelay, OutlinesFadeInDuration));
            } else {
                addActor(createAlignedImage(outlinesStayRegion));
                addActor(createAlignedImage(outlinesColorfullRegion));
            }
        }
        
        private Image createFadeInImage(AtlasRegion region, float delay, float duration) {
            Image image = createAlignedImage(region);
            image.getColor().a = 0;
            image.addAction(delay(delay, fadeIn(duration, Interpolation.fade)));
            return image;
        }
        
        private Image createAlignedImage(AtlasRegion region) {
            Image image = new Image(region);
            image.setPosition(region.offsetX, region.offsetY);
            return image;
        }
        
        @Override
        public void layout() {
            if (getActions().size == 0) {
                Stage stage = getStage();
                float worldWidth = stage.getWidth();
                float worldHeight = stage.getHeight();
                
                setScale(1);
                if (this.width > worldWidth) {
                    setScale((worldWidth - 2 * TitleTopPadding) / this.width);
                }
                setPosition((worldWidth - getPrefWidth()) / 2, worldHeight - getPrefHeight() - TitleTopPadding);
            }
        }
        
        public float getAnimationDuration() {
            return FillingsFadeInDelay + highestAdditionalFillingsFadeInDelay + FillingsFadeInDuration;
        }

        @Override
        public float getPrefWidth() {
            validate();
            return width * getScaleX();
        }

        @Override
        public float getPrefHeight() {
            validate();
            return height * getScaleY();
        }

        @Override
        public float getMaxWidth() {
            return width;
        }

        @Override
        public float getMaxHeight() {
            return height;
        }
        
    }
    
}