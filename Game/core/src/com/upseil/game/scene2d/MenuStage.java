package com.upseil.game.scene2d;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
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
        
        Button startGameButton = new TextButton(hudMessages.get("startGame"), skin, "big");
        startGameButton.addListener(new SimpleChangeListener(() -> {
            ignoreMapper.create(tagManager.getEntityId(Tag.Menu));
            ignoreMapper.remove(tagManager.getEntityId(Tag.Grid));
            ignoreMapper.remove(tagManager.getEntityId(Tag.HUD));
        }));
        
        Table container = new Table(skin);
        container.setFillParent(true);
        container.top();
        
        container.add(new TitleGroup()).expandX().pad(50, 0, 25, 0);
        container.row();
        container.add(startGameButton);
        addActor(container);
    }
    
    private static class TitleGroup extends WidgetGroup {

        private final int width;
        private final int height;
        
        public TitleGroup() {
            TextureAtlas titleAtlas = new TextureAtlas("title/title.atlas");
            
            AtlasRegion backgroundWhiteRegion = titleAtlas.findRegion("background-white");
            this.width = backgroundWhiteRegion.originalWidth;
            this.height = backgroundWhiteRegion.originalHeight;
            
            addActor(createAlignedImage(backgroundWhiteRegion));
            addActor(createAlignedImage(titleAtlas.findRegion("background-black")));
            for (AtlasRegion region : titleAtlas.getRegions()) {
                if (region.name.startsWith("filling-")) {
                    addActor(createAlignedImage(region));
                }
            }
            addActor(createAlignedImage(titleAtlas.findRegion("outlines-stay")));
            addActor(createAlignedImage(titleAtlas.findRegion("outlines-colorful")));
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
