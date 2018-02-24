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
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.HUDConfig;
import com.upseil.game.Tag;
import com.upseil.game.component.GameState;
import com.upseil.game.component.GridComponent;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Grid;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.scene2d.util.TextColor;
import com.upseil.gdx.scene2d.util.ValueLabelBuilder;

@Wire
public class HUDStage extends Stage {
    
    private static final StringBuilder Text = new StringBuilder();
    private static final float ButtonSpacing = 8;
    
    private TagManager<Tag> tagManager;
    private ComponentMapper<GameState> gameStateMapper;
    private ComponentMapper<GridComponent> gridMapper;
    
    @Wire(name="Skin") private Skin skin;
    @Wire(name="UI") private I18NBundle hudMessages;
    private HUDConfig config;
    
    private final Table container;
    
    private GameState gameState;
    private Grid grid;
    
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
        container.add(new Image(BackgroundBuilder.byColor(skin, Color.Color1.getName())))
                 .size(buttonSize, buttonLength).space(ButtonSpacing).expandX().right();
        container.add();
        container.add(new Image(BackgroundBuilder.byColor(skin, Color.Color3.getName())))
                 .size(buttonSize, buttonLength).space(ButtonSpacing).expandX().left();
        
        container.row();
        container.add();
        container.add(new Image(BackgroundBuilder.byColor(skin, Color.Color2.getName())))
                 .size(buttonLength, buttonSize).expandY().top();
        container.add();
        
        addActor(container);
    }
    
    private Actor createHeader() {
        int spacing = 5;
        Table header = new Table(skin);
        header.add(hudMessages.get("score") + ":", "big").expandX().right().padRight(spacing);
        header.add(ValueLabelBuilder.newLabel(skin, "big").withInterval(config.getUpdateInterval())
                                                          .withValue(() -> text().append(gameState.getScore()).toString())
                                    .build())
              .expandX().left();
        return header;
    }

    private Table createCellCounters() {
        float spacing = 5;
        float counterSize = config.getCounterSize();
        Table cellCounters = new Table(skin);
        
        for (int number = 0; number < Color.size(); number++) {
            Color color = Color.forNumber(number);
            cellCounters.add(ValueLabelBuilder.newLabel(skin, "default-bold")
                                              .withInterval(config.getUpdateInterval())
                                              .withValue(() -> text(color.getName()).append(grid.getColorCount(color)).toString())
                                          .build())
                        .expandX().right();
            cellCounters.add(text(color.getName()).append("x"), "default-bold").padLeft(spacing).padRight(spacing);
            cellCounters.add(new Image(BackgroundBuilder.byColor(skin, color.getName()))).size(counterSize).expandX().left();
        }
        
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
        grid = gridMapper.get(tagManager.getEntityId(Tag.Grid)).get();
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
