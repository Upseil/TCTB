package com.upseil.game.scene2d;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.upseil.game.Tag;
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.HUDConfig;
import com.upseil.game.component.GameState;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.util.TextColor;
import com.upseil.gdx.scene2d.util.ValueLabelBuilder;

@Wire
public class HUDStage extends Stage {
    
    private static final StringBuilder Text = new StringBuilder();
    private static final float ButtonSpacing = 8;
    
    private TagManager<Tag> tagManager;
    private ComponentMapper<GameState> gameStateMapper;
    
    @Wire(name="Skin") private Skin skin;
    @Wire(name="UI") private I18NBundle hudMessages;
    private HUDConfig config;
    
    private final Table container;
    
    private GameState gameState;
    
    public HUDStage(Batch batch, World world) {
        super(new ScreenViewport(), batch);
        world.inject(this);
        
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getHUDConfig();
        float buttonSize = config.getButtonSize();
        float buttonLength = config.getButtonLength();
        
        container = new Table(skin);
        container.setFillParent(true);
        container.pad(config.getPadding());
        
        container.add(createHeader()).padBottom(10).colspan(3).expandY().bottom();

        container.row();
        container.add();
        container.add(createCellCounters()).fillX();
        container.add();

        container.row();
        container.add(new Image(skin, "color0")).size(buttonSize, buttonLength).space(ButtonSpacing).expandX().right();
        container.add();
        container.add(new Image(skin, "color2")).size(buttonSize, buttonLength).space(ButtonSpacing).expandX().left();
        
        container.row();
        container.add();
        container.add(new Image(skin, "color1")).size(buttonLength, buttonSize).expandY().top();
        container.add();
        
        addActor(container);
    }
    
    private Actor createHeader() {
        int spacing = 5;
        Table header = new Table(skin);
        header.add(hudMessages.get("score") + ":", "big").expandX().right().padRight(spacing);
        header.add(ValueLabelBuilder.newLabel(skin, "big").withInterval(config.getUpdateInterval())
                                                          .withValue(() -> Integer.toString(gameState.getScore()))
                                    .build())
              .expandX().left();
        return header;
    }

    private Table createCellCounters() {
        float spacing = 5;
        float counterSize = config.getCounterSize();
        Table cellCounters = new Table(skin);
        
        // TODO Use colored value labels
        cellCounters.add(text("t-color0").append("123"), "default-bold").expandX().right();
        cellCounters.add(text("t-color0").append("x"), "default-bold").padLeft(spacing).padRight(spacing);
        cellCounters.add(new Image(skin, "color0")).size(counterSize).expandX().left();

        cellCounters.add(text("t-color1").append("456"), "default-bold").expandX().right();
        cellCounters.add(text("t-color1").append("x"), "default-bold").padLeft(spacing).padRight(spacing);
        cellCounters.add(new Image(skin, "color1")).size(counterSize).expandX().left();

        cellCounters.add(text("t-color2").append("789"), "default-bold").expandX().right();
        cellCounters.add(text("t-color2").append("x"), "default-bold").padLeft(spacing).padRight(spacing);
        cellCounters.add(new Image(skin, "color2")).size(counterSize).expandX().left();
        
        return cellCounters;
    }

    private StringBuilder text() {
        Text.setLength(0);
        return Text;
    }
    
    private StringBuilder text(String color) {
        return text().append(TextColor.byName(color).asMarkup());
    }
    
    @Override
    public void act(float delta) {
        gameState = gameStateMapper.get(tagManager.getEntityId(Tag.GameState));
        super.act(delta);
    }
    
    public float getLeftWidth() {
        container.validate();
        return container.getColumnWidth(0) + config.getPadding();
    }
    
    public float getRightWidth() {
        container.validate();
        return container.getColumnWidth(container.getColumns() - 1) + config.getPadding();
    }
    
    public float getTopHeight() {
        container.validate();
        return container.getRowHeight(0) + container.getRowHeight(1) + config.getPadding();
    }
    
    public float getBottomHeight() {
        container.validate();
        return container.getRowHeight(container.getRows() - 1) + config.getPadding();
    }
    
}
