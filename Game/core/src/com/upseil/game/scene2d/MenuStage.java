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
import com.upseil.game.Tag;
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
        
        TitleGroup title = new TitleGroup(viewport.getWorldWidth());
        title.setPosition((viewport.getWorldWidth() - title.getPrefWidth()) / 2, (viewport.getWorldHeight() - title.getPrefHeight()) / 2);
        title.addAction(delay(title.getAnimationDuration(),
                moveTo(title.getX(), getHeight() - title.getPrefHeight() - TitleTopPadding, TitleMoveDuration, Interpolation.swing)));
        
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

        controls.getColor().a = 0;
        controls.addAction(delay(title.getAnimationDuration() + ControlsFadeInDelay, fadeIn(ControlsFadeInDuration)));
        
        addActor(controls);
        addActor(title);
    }
    
    private static class TitleGroup extends WidgetGroup {
        
        private static final float FlyInDelay = 0.5f;
        private static final float FlyInDuration = 1.5f;
        private static final Interpolation FlyInInterpolation = Interpolation.swingOut;
        
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
        
        public TitleGroup(float worldWidth) {
            TextureAtlas titleAtlas = new TextureAtlas("title/title.atlas");
            
            AtlasRegion backgroundWhiteRegion = titleAtlas.findRegion("background-white");
            AtlasRegion backgroundBlackRegion = titleAtlas.findRegion("background-black");
            this.width = backgroundWhiteRegion.originalWidth;
            this.height = backgroundWhiteRegion.originalHeight;
            
            float slope = (float) height / (backgroundWhiteRegion.packedWidth + backgroundBlackRegion.packedWidth - width);

            float backgroundWhiteStartX = worldWidth * -1.0f;
            float backgroundWhiteStartY = slope * backgroundWhiteStartX;
            Image backgroundWhite = new Image(backgroundWhiteRegion);
            backgroundWhite.setPosition(backgroundWhiteStartX, backgroundWhiteStartY);
            backgroundWhite.addAction(delay(FlyInDelay, moveTo(backgroundWhiteRegion.offsetX, backgroundWhiteRegion.offsetY, FlyInDuration, FlyInInterpolation)));
            addActor(backgroundWhite);
            
            float backgroundBlackStartX = backgroundWhiteStartX * -1 + backgroundBlackRegion.offsetX;
            float backgroundBlackStartY = -1 * backgroundWhiteStartY;
            Image backgroundBlack = new Image(backgroundBlackRegion);
            backgroundBlack.setPosition(backgroundBlackStartX, backgroundBlackStartY);
            backgroundBlack.addAction(delay(FlyInDelay, moveTo(backgroundBlackRegion.offsetX, backgroundBlackRegion.offsetY, FlyInDuration, FlyInInterpolation)));
            addActor(backgroundBlack);
            
            for (AtlasRegion region : titleAtlas.getRegions()) {
                if (region.name.startsWith("filling-")) {
                    float additionalDelay = (region.offsetX / width) * MaxAdditionalFillingsFadeInDelay;
                    if (additionalDelay > highestAdditionalFillingsFadeInDelay) {
                        highestAdditionalFillingsFadeInDelay = additionalDelay;
                    }
                    float totalDelay = FillingsFadeInDelay + additionalDelay;
                    addActor(createFadeInImage(region, totalDelay, FlyInDuration));
                }
            }

            addActor(createFadeInImage(titleAtlas.findRegion("outlines-stay"), OutlinesFadeInDelay, OutlinesFadeInDuration));
            addActor(createFadeInImage(titleAtlas.findRegion("outlines-colorful"), OutlinesFadeInDelay, OutlinesFadeInDuration));
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
                setPosition((stageWidth - width) / 2, stageHeight - height - TitleTopPadding);
            }
        }
        
        public float getAnimationDuration() {
            return FillingsFadeInDelay + highestAdditionalFillingsFadeInDelay + FillingsFadeInDuration;
        }

        @Override
        public float getPrefWidth() {
            return width;
        }

        @Override
        public float getPrefHeight() {
            return height;
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