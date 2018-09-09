package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.upseil.game.Config.MenuConfigValues.AdditionalOutlinesFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.BackgroundAlpha;
import static com.upseil.game.Config.MenuConfigValues.BackgroundBlurFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.BackgroundFadeOutDuration;
import static com.upseil.game.Config.MenuConfigValues.ControlsFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.ControlsFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.ControlsTopPadding;
import static com.upseil.game.Config.MenuConfigValues.FillingsFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.FlyInDelay;
import static com.upseil.game.Config.MenuConfigValues.FlyInDuration;
import static com.upseil.game.Config.MenuConfigValues.GlassAlpha;
import static com.upseil.game.Config.MenuConfigValues.GridSize;
import static com.upseil.game.Config.MenuConfigValues.LogoAnimation;
import static com.upseil.game.Config.MenuConfigValues.LogoHorizontalPadding;
import static com.upseil.game.Config.MenuConfigValues.LogoMoveDuration;
import static com.upseil.game.Config.MenuConfigValues.LogoTopPadding;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalFillingsFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.OutlinesFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.ShadowFadeInDuration;

import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ObjectMap;
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
    
    // TODO Extract
    public static class LogoGroup extends WidgetGroup implements Disposable {
        
        private final AnimationStyle animationStyle;
        private final TextureAtlas atlas;
        private final ObjectMap<String, AtlasRegion> regionMap;

        private final int width;
        private final int height;
        
        private float highestAdditionalFillingsFadeInDelay;
        private boolean hasOutlines;
        private boolean hasShadows;
        private boolean hasBlur;
        
        public LogoGroup(float worldWidth, AnimationStyle animationStyle) {
            this.animationStyle = animationStyle;
            atlas = new TextureAtlas("title/title.atlas");
            this.width = atlas.getRegions().get(0).originalWidth;
            this.height = atlas.getRegions().get(0).originalHeight;
            
            regionMap = new ObjectMap<>();
            for (AtlasRegion region : atlas.getRegions()) {
                regionMap.put(region.name, region);
                if (region.name.startsWith("filling-")) {
                    float additionalDelay = calculateAdditionalFillingsFadeInDelay(region);
                    if (additionalDelay > highestAdditionalFillingsFadeInDelay) {
                        highestAdditionalFillingsFadeInDelay = additionalDelay;
                    }
                }
                
                hasOutlines |= region.name.startsWith("outline");
                hasShadows |= region.name.startsWith("shadow");
                hasBlur |= region.name.endsWith("blur");
            }
            
            addBackground(worldWidth);
            addFillings();
            addOutlines();
        }
        
        private void addBackground(float worldWidth) {
            addBackgroundBlur();

            AtlasRegion backgroundWhiteRegion = regionMap.get("background-white");
            AtlasRegion backgroundBlackRegion = regionMap.get("background-black");

            Image backgroundWhite = createAlignedImage(backgroundWhiteRegion);
            addActor(backgroundWhite);
            Image backgroundBlack = createAlignedImage(backgroundBlackRegion);
            addActor(backgroundBlack);
            
            float slope = (float) height / (backgroundWhiteRegion.packedWidth + backgroundBlackRegion.packedWidth - width);
            float delay = animationStyle.flyInDelay;
            float duration = animationStyle.flyInDuration;
            Interpolation interpolation = new Swing2Out();
            
            float backgroundWhiteStartX = worldWidth * -1.25f;
            float backgroundWhiteStartY = slope * backgroundWhiteStartX;
            backgroundWhite.setPosition(backgroundWhiteStartX, backgroundWhiteStartY);
            backgroundWhite.addAction(
                delay(delay, moveTo(backgroundWhiteRegion.offsetX, backgroundWhiteRegion.offsetY, duration, interpolation))
            );
            
            float backgroundBlackStartX = backgroundWhiteStartX * -1 + backgroundBlackRegion.offsetX;
            float backgroundBlackStartY = -1 * backgroundWhiteStartY;
            backgroundBlack.setPosition(backgroundBlackStartX, backgroundBlackStartY);
            backgroundBlack.addAction(
                delay(delay, moveTo(backgroundBlackRegion.offsetX, backgroundBlackRegion.offsetY, duration, interpolation))
            );
            
            float afterEffectsDelay = calculateAfterEffectsDelay();
            float afterAlpha = animationStyle.backgroundAlpha;
            backgroundWhite.addAction(delay(afterEffectsDelay, alpha(afterAlpha, animationStyle.backgroundFadeOutDuration, Interpolation.fade)));
            backgroundBlack.addAction(delay(afterEffectsDelay, alpha(afterAlpha, animationStyle.backgroundFadeOutDuration, Interpolation.fade)));
        }

        private void addBackgroundBlur() {
            AtlasRegion region = regionMap.get("background-blur");
            if (region != null) {
                Image backgroundBlur = createFadeInImage(region, calculateAfterEffectsDelay(), animationStyle.backgroundBlurFadeInDuration);
                backgroundBlur.moveBy(
                    (width - backgroundBlur.getPrefWidth()) / 2,
                    (height - backgroundBlur.getPrefHeight()) / 2
                );
                addActor(backgroundBlur);
            }
        }
        
        private void addFillings() {
            // TODO After Effects not as part of the animation, but when the logo has already moved into place
            float afterEffectsDelay = calculateAfterEffectsDelay();
            float fillingsDelay = animationStyle.flyInDelay + animationStyle.flyInDuration;
            if (hasOutlines) {
                fillingsDelay += animationStyle.additionalOutlinesFadeInDelay + animationStyle.outlinesFadeInDuration * 0.5f;
            }
            
            for (AtlasRegion fillingRegion : regionMap.values()) {
                if (fillingRegion.name.startsWith("filling-")) {
                    String shadowName = "shadow" + fillingRegion.name.substring(fillingRegion.name.indexOf('-'));
                    AtlasRegion shadowRegion = regionMap.get(shadowName);
                    
                    if (shadowRegion != null) {
                        addActor(createFadeInImage(shadowRegion, afterEffectsDelay, animationStyle.shadowFadeInDuration));
                    }
                    float totalFillingDelay = fillingsDelay + calculateAdditionalFillingsFadeInDelay(fillingRegion);
                    addActor(createFadeInImage(fillingRegion, totalFillingDelay, animationStyle.fillingsFadeInDuration));
                }
            }
        }

        private void addOutlines() {
            AtlasRegion outlinesStayRegion = regionMap.get("outlines-stay");
            AtlasRegion outlinesColorfulRegion = regionMap.get("outlines-colorful");
            if (outlinesStayRegion != null && outlinesColorfulRegion != null) {
                float delay = animationStyle.flyInDelay + animationStyle.flyInDuration + animationStyle.additionalOutlinesFadeInDelay;
                float duration = animationStyle.outlinesFadeInDuration;
                Image outlinesStay = createFadeInImage(outlinesStayRegion, delay, duration);
                Image outlinesColorful = createFadeInImage(outlinesColorfulRegion, delay, duration);
                addActor(outlinesStay);
                addActor(outlinesColorful);
            }
        }
        
        private Image createFadeInImage(AtlasRegion region, float delay, float duration) {
            Image image = createAlignedImage(region);
            setupFadeInAction(image, delay, duration);
            return image;
        }

        private void setupFadeInAction(Image image, float delay, float duration) {
            image.getColor().a = 0;
            image.addAction(delay(delay, fadeIn(duration, Interpolation.fade)));
        }
        
        private Image createAlignedImage(AtlasRegion region) {
            Image image = new Image(region);
            image.setPosition(region.offsetX, region.offsetY);
            return image;
        }
        
        private float calculateAdditionalFillingsFadeInDelay(AtlasRegion region) {
            return (region.offsetX / width) * animationStyle.maxAdditionalFillingsFadeInDelay;
        }
        
        public float getAnimationDuration() {
            float totalShadowsDuration = hasShadows ? animationStyle.shadowFadeInDuration : 0;
            float totalBlurDuration = hasBlur ? animationStyle.backgroundBlurFadeInDuration : 0;
            return calculateAfterEffectsDelay() + totalShadowsDuration + totalBlurDuration;
        }
        
        private float calculateAfterEffectsDelay() {
            float totalFlyInDuration = animationStyle.flyInDelay + animationStyle.flyInDuration;
            float totalFillingsDuration = highestAdditionalFillingsFadeInDelay + animationStyle.fillingsFadeInDuration * 0.5f;
            float totalOutlinesDuration = 0;
            if (hasOutlines) {
                totalOutlinesDuration = animationStyle.additionalOutlinesFadeInDelay + animationStyle.outlinesFadeInDuration * 0.5f;
            }
            return totalFlyInDuration + totalOutlinesDuration + totalFillingsDuration;
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

        @Override
        public void dispose() {
            atlas.dispose();
        }
        
        public static class AnimationStyle {
            
            public final float flyInDelay;
            public final float flyInDuration;
            public final float backgroundAlpha;
            public final float backgroundFadeOutDuration;
            public final float additionalOutlinesFadeInDelay;
            public final float outlinesFadeInDuration;
            public final float fillingsFadeInDuration;
            public final float maxAdditionalFillingsFadeInDelay;
            public final float backgroundBlurFadeInDuration;
            public final float shadowFadeInDuration;
            
            public static AnimationStyle from(MenuConfig config) {
                if (config.getBoolean(LogoAnimation)) {
                    return new AnimationStyle(config.getFloat(FlyInDelay), config.getFloat(FlyInDuration), config.getFloat(BackgroundAlpha),
                            config.getFloat(BackgroundFadeOutDuration), config.getFloat(AdditionalOutlinesFadeInDelay), config.getFloat(OutlinesFadeInDuration),
                            config.getFloat(FillingsFadeInDuration), config.getFloat(MaxAdditionalFillingsFadeInDelay),
                            config.getFloat(BackgroundBlurFadeInDuration), config.getFloat(ShadowFadeInDuration));
                } else {
                    return new AnimationStyle(0, 0, config.getFloat(BackgroundAlpha), 0, 0, 0, 0, 0, 0, 0);
                }
            }
            
            public AnimationStyle(float flyInDelay, float flyInDuration, float backgroundAlpha, float backgroundFadeOutDuration,
                    float additionalOutlinesFadeInDelay, float outlinesFadeInDuration, float fillingsFadeInDuration, float maxAdditionalFillingsFadeInDelay,
                    float backgroundBlurFadeInDuration, float shadowFadeInDuration) {
                this.flyInDelay = flyInDelay;
                this.flyInDuration = flyInDuration;
                this.backgroundAlpha = backgroundAlpha;
                this.backgroundFadeOutDuration = backgroundFadeOutDuration;
                this.additionalOutlinesFadeInDelay = additionalOutlinesFadeInDelay;
                this.outlinesFadeInDuration = outlinesFadeInDuration;
                this.fillingsFadeInDuration = fillingsFadeInDuration;
                this.maxAdditionalFillingsFadeInDelay = maxAdditionalFillingsFadeInDelay;
                this.backgroundBlurFadeInDuration = backgroundBlurFadeInDuration;
                this.shadowFadeInDuration = shadowFadeInDuration;
            }
            
        }
        
    }
    
}