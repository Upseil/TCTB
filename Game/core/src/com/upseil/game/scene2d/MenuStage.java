package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Interpolation;
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
import com.upseil.game.Config.MenuConfigValues;
import com.upseil.game.Constants.Tag;
import com.upseil.game.math.Swing2Out;
import com.upseil.gdx.artemis.component.Ignore;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.SimpleChangeListener;

@Wire
public class MenuStage extends Stage {
    
    private static final float TitleTopPadding = 50;
    private static final float TitleMoveDuration = 1f;
    
    private static final float ControlsTopPadding = 100;
    private static final float ControlsFadeInDelay = TitleMoveDuration / 2;
    private static final float ControlsFadeInDuration = 1f;

    private TagManager<Tag> tagManager;
    private ComponentMapper<Ignore> ignoreMapper;
    
    @Wire(name="Skin") private Skin skin;
    @Wire(name="UI") private I18NBundle hudMessages;
    
    public MenuStage(Viewport viewport, Batch batch, World world) {
        super(viewport, batch);
        world.inject(this);
        
        GameConfig gameConfig = world.getRegistered("Config");
        MenuConfig config = gameConfig.getMenuConfig();
        boolean titleAnimation = config.getBoolean(MenuConfigValues.TitleAnimation);
        
        TitleGroup title = new TitleGroup(viewport.getWorldWidth(), titleAnimation);
        if (titleAnimation) {
            title.setPosition((viewport.getWorldWidth() - title.getPrefWidth()) / 2, (viewport.getWorldHeight() - title.getPrefHeight()) / 2);
            title.addAction(delay(title.getAnimationDuration(),
                    moveTo(title.getX(), getHeight() - title.getPrefHeight() - TitleTopPadding, TitleMoveDuration, Interpolation.swing)));
        }
        
        Button startGameButton = new TextButton(hudMessages.get("startGame"), skin, "menu2");
        startGameButton.addListener(new SimpleChangeListener(() -> {
            ignoreMapper.create(tagManager.getEntityId(Tag.Menu));
            ignoreMapper.remove(tagManager.getEntityId(Tag.Grid));
            ignoreMapper.remove(tagManager.getEntityId(Tag.HUD));
        }));
        
        Button exitButton = null;
        ApplicationType applicationType = Gdx.app.getType();
        if (applicationType == ApplicationType.Android || applicationType == ApplicationType.Desktop) {
            exitButton = new TextButton(hudMessages.get("exit"), skin, "menu1");
            exitButton.addListener(new SimpleChangeListener(() -> Gdx.app.exit()));
        }
        
        Table controls = new Table(skin);
        controls.setFillParent(true);
        controls.top().padTop(TitleTopPadding + title.getPrefHeight() + ControlsTopPadding);
        
        controls.defaults().space(25).fillX();
        controls.add(startGameButton);
        if (exitButton != null) {
            controls.row();
            controls.add(exitButton);
        }

        if (titleAnimation) {
            controls.getColor().a = 0;
            controls.addAction(delay(title.getAnimationDuration() + ControlsFadeInDelay, fadeIn(ControlsFadeInDuration)));
        }
        
        addActor(controls);
        addActor(title);
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
        private float stageWidth;
        private float stageHeight;
        
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
            image.addAction(delay(delay, fadeIn(duration)));
            return image;
        }
        
        private Image createAlignedImage(AtlasRegion region) {
            Image image = new Image(region);
            image.setPosition(region.offsetX, region.offsetY);
            return image;
        }
        
        @Override
        public void validate() {
            Stage stage = getStage();
            if (getActions().size == 0 && stage != null) {
                float stageWidth = stage.getWidth();
                float stageHeight = stage.getHeight();
                if (this.stageWidth != stageWidth || this.stageHeight != stageHeight) {
                    invalidate();
                }
            }
            super.validate();
        }
        
        @Override
        public void layout() {
            if (getActions().size == 0) {
                Stage stage = getStage();
                stageWidth = stage.getWidth();
                stageHeight = stage.getHeight();
                setPosition((stageWidth - getPrefWidth()) / 2, stageHeight - getPrefHeight() - TitleTopPadding);
            }
        }
        
        public float getAnimationDuration() {
            return FillingsFadeInDelay + highestAdditionalFillingsFadeInDelay + FillingsFadeInDuration;
        }

        @Override
        public float getPrefWidth() {
            return width * getScaleX();
        }

        @Override
        public float getPrefHeight() {
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