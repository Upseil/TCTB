package com.upseil.game.scene2d;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.HUDConfig;
import com.upseil.game.Tag;
import com.upseil.game.component.GameState;
import com.upseil.game.domain.Color;
import com.upseil.game.system.GridController;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.scene2d.util.TextColor;
import com.upseil.gdx.scene2d.util.ValueLabelBuilder;

@Wire
public class HUDStage extends Stage {
    
    private static final StringBuilder Text = new StringBuilder();
    private static final float ButtonSpacing = 8;
    
    private TagManager<Tag> tagManager;
    private GridController gridController;
    
    private ComponentMapper<GameState> gameStateMapper;
    
    @Wire(name="Skin") private Skin skin;
    @Wire(name="UI") private I18NBundle hudMessages;
    private HUDConfig config;
    
    private final Table container;
    private final Button[] buttons;
    
    private GameState gameState;
    
    private boolean updateValueLabels;
    private boolean continousUpdate;
    
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
        
        buttons = new Button[3];

        Button button0 = new Button(skin, "button0");
        button0.addListener(new ButtonListener(this, gridController, Color.Color0, button0));
        buttons[0] = button0;

        Button button1 = new Button(skin, "button1");
        button1.addListener(new ButtonListener(this, gridController, Color.Color1, button1));
        buttons[1] = button1;

        Button button2 = new Button(skin, "button2");
        button2.addListener(new ButtonListener(this, gridController, Color.Color2, button2));
        buttons[2] = button2;
        
        container.add(createHeader()).padBottom(10).colspan(3).expandY().bottom();

        container.row();
        container.add();
        container.add(createCellCounters()).fillX();
        container.add();

        container.row();
        container.add(button0).size(buttonSize, buttonLength).space(ButtonSpacing).expandX().right();
        container.add();
        container.add(button2).size(buttonSize, buttonLength).space(ButtonSpacing).expandX().left();
        
        container.row();
        container.add();
        container.add(button1).size(buttonLength, buttonSize).expandY().top();
        container.add();
        
        addActor(container);
        
        updateValueLabels = true;
    }
    
    private Actor createHeader() {
        int spacing = 5;
        Table header = new Table(skin);
        header.add(hudMessages.get("score") + ":", "big").expandX().right().padRight(spacing);
        header.add(ValueLabelBuilder.newLabel(skin, "big").updateIf(this::updateValueLabels)
                                                          .withValue(() -> text().append(gameState.getScore()).toString())
                                    .build())
              .expandX().left();
        return header;
    }

    private Table createCellCounters() {
        float spacing = 5;
        float counterSize = config.getCounterSize();
        int expectedColorCount = gridController.getExpectedColorCount();
        StringBuilder dummyText = text().append('0'); // One more than expected
        do {
            dummyText.append('0');
            expectedColorCount /= 10;
        } while (expectedColorCount != 0);
        Label widthProvider = new Label(dummyText.toString(), skin, "default-bold");
        float minWidth = widthProvider.getPrefWidth();
        
        Table cellCounters = new Table(skin);
        for (int n = 0; n < Color.size(); n++) {
            final int number = n;
            Label countLabel = new Label(null, skin, "default-bold");
            countLabel.setAlignment(Align.right);
            cellCounters.add(ValueLabelBuilder.decorate(countLabel)
                                              .updateIf(this::updateValueLabels)
                                              .withValue(() -> text(getTextColor(number)).append(gridController.getColorCount(number)).toString())
                                          .build())
                        .expandX().right().minWidth(minWidth);
            cellCounters.add(ValueLabelBuilder.newLabel(skin, "default-bold")
                                              .updateIf(this::updateValueLabels)
                                              .withValue(() -> text(getTextColor(number)).append("x").toString())
                                          .build())
                        .padLeft(spacing).padRight(spacing);
            cellCounters.add(new Image(BackgroundBuilder.byColor(skin, Color.forNumber(number).getName()))).size(counterSize).expandX().left();
        }
        
        return cellCounters;
    }

    private String getTextColor(int colorNumber) {
        Color color = Color.forNumber(colorNumber);
        StringBuilder colorName = text();
        colorName.append(color.getName());
        
        Button button = buttons[colorNumber];
        if (button.isOver() && !button.isDisabled()) {
            colorName.append("-emphasize");
        }
        return colorName.toString();
    }

    private StringBuilder text() {
        Text.setLength(0);
        return Text;
    }
    
    private StringBuilder text(String color) {
        return text().append(TextColor.byName(color).asMarkup());
    }

    public void setButtonsDisabled(boolean disabled) {
        for (Button button : buttons) {
            button.setDisabled(disabled);
        }
    }
    
    private boolean updateValueLabels() {
        return updateValueLabels || continousUpdate;
    }

    public void setUpdateValueLabels(boolean updateValueLabels) {
        this.updateValueLabels = updateValueLabels;
    }

    public void setContinousUpdate(boolean continousUpdate) {
        this.continousUpdate = continousUpdate;
    }

    @Override
    public void act(float delta) {
        gameState = gameStateMapper.get(tagManager.getEntityId(Tag.GameState));
        super.act(delta);
        updateValueLabels = false;
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
