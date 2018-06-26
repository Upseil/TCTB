package com.upseil.game.scene2d;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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
        container.add(startGameButton);
        addActor(container);
    }
    
}
