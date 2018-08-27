package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
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

    private TagManager<Tag> tagManager;
    private ComponentMapper<Ignore> ignoreMapper;
    
    @Wire(name="Skin") private Skin skin;
    @Wire(name="UI") private I18NBundle hudMessages;
    
    public MenuStage(Viewport viewport, Batch batch, World world) {
        super(viewport, batch);
        world.inject(this);
        
        TitleGroup title = new TitleGroup(viewport.getWorldWidth());
        
        Button startGameButton = new TextButton(hudMessages.get("startGame"), skin, "big");
        startGameButton.addListener(new SimpleChangeListener(() -> {
            ignoreMapper.create(tagManager.getEntityId(Tag.Menu));
            ignoreMapper.remove(tagManager.getEntityId(Tag.Grid));
            ignoreMapper.remove(tagManager.getEntityId(Tag.HUD));
        }));
        
        Table container = new Table(skin);
        container.setFillParent(true);
        container.top();
        
        container.add(title).expandX().pad(50, 0, 25, 0);
        container.row();
        container.add(startGameButton);
        addActor(container);
    }
    
    private static class TitleGroup extends WidgetGroup {
        
        private static final float ActionDelay = 0.1f;
        private static final float FlyInDuration = 1.5f;
        private static final float OutlinesFadeInDuration = 1f;
        private static final float FillingsFadeInDuration = 1f;
        private static final float MaxFillingsFadeInDelay = 0.25f;

        private final int width;
        private final int height;
        
        public TitleGroup(float worldWidth) {
            TextureAtlas titleAtlas = new TextureAtlas("title/title.atlas");
            
            AtlasRegion backgroundWhiteRegion = titleAtlas.findRegion("background-white");
            AtlasRegion backgroundBlackRegion = titleAtlas.findRegion("background-black");
            this.width = backgroundWhiteRegion.originalWidth;
            this.height = backgroundWhiteRegion.originalHeight;
            
            float slope = (float) height / (backgroundWhiteRegion.packedWidth + backgroundBlackRegion.packedWidth - width);
            
            float backgroundWhiteStartX = worldWidth / 4;
            Image backgroundWhite = new Image(backgroundWhiteRegion);
            backgroundWhite.setPosition(-backgroundWhiteStartX, slope * -backgroundWhiteStartX);
            backgroundWhite.addAction(moveTo(backgroundWhiteRegion.offsetX, backgroundWhiteRegion.offsetY, FlyInDuration, Interpolation.swingOut));
            addActor(backgroundWhite);
            
            float backgroundBlackStartX = backgroundWhiteStartX + 2 * backgroundBlackRegion.offsetX;
            Image backgroundBlack = new Image(backgroundBlackRegion);
            backgroundBlack.setPosition(backgroundBlackStartX, (slope * backgroundBlackStartX) - backgroundBlackRegion.offsetX);
            backgroundBlack.addAction(moveTo(backgroundBlackRegion.offsetX, backgroundBlackRegion.offsetY, FlyInDuration, Interpolation.swingOut));
            addActor(backgroundBlack);
            
            for (AtlasRegion region : titleAtlas.getRegions()) {
                if (region.name.startsWith("filling-")) {
                    float delay = FlyInDuration + ActionDelay + OutlinesFadeInDuration + MathUtils.random(MaxFillingsFadeInDelay);
                    Image filling = createAlignedImage(region);
                    filling.getColor().a = 0;
                    filling.addAction(delay(delay, fadeIn(FillingsFadeInDuration)));
                    addActor(filling);
                }
            }
            
            Image outlinesStay = createAlignedImage(titleAtlas.findRegion("outlines-stay"));
            outlinesStay.getColor().a = 0;
            outlinesStay.addAction(delay(FlyInDuration + ActionDelay, fadeIn(OutlinesFadeInDuration)));
            addActor(outlinesStay);
            
            Image outlinesColorful = createAlignedImage(titleAtlas.findRegion("outlines-colorful"));
            outlinesColorful.getColor().a = 0;
            outlinesColorful.addAction(delay(FlyInDuration + ActionDelay, fadeIn(OutlinesFadeInDuration)));
            addActor(outlinesColorful);
        }
        
        private Image createAlignedImage(AtlasRegion region) {
            Image image = new Image(region);
            image.setPosition(region.offsetX, region.offsetY);
            return image;
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
