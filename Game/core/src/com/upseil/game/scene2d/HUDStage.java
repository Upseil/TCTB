package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;
import static com.upseil.game.Config.HUDConfigValues.ButtonRatio;
import static com.upseil.game.Config.HUDConfigValues.CounterSize;
import static com.upseil.game.Config.HUDConfigValues.Padding;
import static com.upseil.gdx.scene2d.util.Values.floatValue;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.upseil.game.Config.GameConfig;
import com.upseil.game.Config.HUDConfig;
import com.upseil.game.Constants.Tag;
import com.upseil.game.component.GameState;
import com.upseil.game.domain.Color;
import com.upseil.game.event.CellsAddedEvent;
import com.upseil.game.event.CellsChangedEvent;
import com.upseil.game.event.CellsRemovedEvent;
import com.upseil.game.system.GridController;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.SimpleKeyInputListener;
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
    private float buttonRatio;
    
    private GameState gameState;
    
    private boolean updateValueLabels;
    private boolean buttonsDisabled;
    
    public HUDStage(Viewport viewport, Batch batch, World world) {
        super(viewport, batch);
        world.inject(this);
        
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getHUDConfig();
        buttonRatio = config.getFloat(ButtonRatio);
        
        EventSystem eventSystem = world.getSystem(EventSystem.class);
        eventSystem.registerHandler(CellsRemovedEvent.Type, event -> {
            gameState.incrementScore(event.getCount());
            setUpdateValueLabels(true);
        });
        eventSystem.registerHandler(CellsAddedEvent.Type, e -> setUpdateValueLabels(true));
        eventSystem.registerHandler(CellsChangedEvent.Type, e -> setUpdateValueLabels(true));
        
        addListener(new KeyPressListener());
        
        container = new Table(skin);
        container.setFillParent(true);
        container.pad(config.getFloat(Padding));
        
        buttons = new Button[3];
        for (int number = 0; number < Color.size(); number++) {
            Button button = new Button(skin, text().append("button").append(number).toString());
            button.addListener(new ButtonListener(this, gridController, Color.forNumber(number), button));
            buttons[number] = button;
        }
        
        container.add(createHeader()).colspan(3).expandY().bottom();

        container.row();
        container.add();
        container.add(createCellCounters()).fillX();
        container.add();

        container.row();
        container.add(buttons[0]).size(floatValue(this::calculateButtonSize), floatValue(this::calculateButtonLength)).space(ButtonSpacing).right();
        container.add();
        container.add(buttons[2]).size(floatValue(this::calculateButtonSize), floatValue(this::calculateButtonLength)).space(ButtonSpacing).left();
        
        container.row();
        container.add();
        container.add(buttons[1]).size(floatValue(this::calculateButtonLength), floatValue(this::calculateButtonSize)).space(ButtonSpacing).top();
        container.add();
        
        addActor(container);
        updateValueLabels = true;
    }
    
    // FIXME Somehow this breaks when the screen height is much bigger than the screen width
    private float calculateButtonLength() {
        return getWidth() / (1 + 2 * buttonRatio) - container.getPadLeft() - container.getPadRight() - 2 * ButtonSpacing;
    }
    
    private float calculateButtonSize() {
        return calculateButtonLength() * buttonRatio;
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
        float counterSize = config.getFloat(CounterSize);
        int expectedColorCount = gridController.getExpectedColorCount();
        StringBuilder dummyText = text().append('0'); // One more than expected
        do {
            dummyText.append('0');
            expectedColorCount /= 10;
        } while (expectedColorCount != 0);
        Label widthProvider = new Label(dummyText.toString(), skin, "default");
        float minWidth = widthProvider.getPrefWidth();
        
        Table cellCounters = new Table(skin);
        for (int n = 0; n < Color.size(); n++) {
            final int number = n;
            Label countLabel = new Label(null, skin, "default");
            countLabel.setAlignment(Align.right);
            cellCounters.add(ValueLabelBuilder.decorate(countLabel)
                                              .updateIf(this::updateValueLabels)
                                              .withValue(() -> text(getTextColor(number)).append(gridController.getColorCount(number)).toString())
                                          .build())
                        .expandX().right().minWidth(minWidth);
            cellCounters.add(ValueLabelBuilder.newLabel(skin, "default")
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

    @Override
    public void act(float delta) {
        gameState = gameStateMapper.get(tagManager.getEntityId(Tag.GameState));
        super.act(delta);
        if (updateValueLabels) {
            updateButtonsDisabled();
        }
        updateValueLabels = false;
    }

    public void setButtonsDisabled(boolean buttonsDisabled) {
        this.buttonsDisabled = buttonsDisabled;
        updateButtonsDisabled();
    }
    
    private void updateButtonsDisabled() {
        for (int index = 0; index < buttons.length; index++) {
            buttons[index].setDisabled(buttonsDisabled || gridController.getColorCount(index) <= 0);
        }
    }

    private boolean updateValueLabels() {
        return updateValueLabels;
    }

    public void setUpdateValueLabels(boolean updateValueLabels) {
        this.updateValueLabels = updateValueLabels;
    }
    
    public float getLeftWidth() {
        container.validate();
        return container.getColumnWidth(0) + container.getPadLeft();
    }
    
    public float getRightWidth() {
        container.validate();
        return container.getColumnWidth(container.getColumns() - 1) + container.getPadRight();
    }
    
    public float getTopHeight() {
        container.validate();
        return container.getRowHeight(0) + container.getRowHeight(1) + container.getPadTop();
    }
    
    public float getBottomHeight() {
        container.validate();
        return container.getRowHeight(container.getRows() - 1) + container.getPadBottom();
    }
    
    private class KeyPressListener extends SimpleKeyInputListener {
        
        private final Vector2 position = new Vector2();
        private Button armedButton;
        
        @Override
        public boolean keyDown(InputEvent event, int keyCode) {
            if (keyCode == Keys.ESCAPE && armedButton != null) {
                mouseMoved(Gdx.input.getX(), Gdx.input.getY());
                armedButton = null;
                return true;
            }

            boolean handled = false;
            switch (keyCode) {
            case Keys.LEFT:
                armedButton = buttons[0];
                break;
            case Keys.DOWN:
                armedButton = buttons[1];
                break;
            case Keys.RIGHT:
                armedButton = buttons[2];
                break;
            default:
                break;
            }
            if (armedButton != null) {
                updatePosition();
                mouseMoved((int) position.x, (int) position.y);
                handled = true;
            }
            return handled;
        }
        
        @Override
        public boolean keyUp(InputEvent event, int keyCode) {
            boolean handled = false;
            if (armedButton != null && isKeyCodeValid(keyCode)) {
                updatePosition();
                int screenX = (int) position.x;
                int screenY = (int) position.y;
                touchDown(screenX, screenY, 0, Buttons.LEFT);
                addAction(delay(0.1f, run(() -> {
                    touchUp(screenX, screenY, 0, Buttons.LEFT);
                    mouseMoved(Gdx.input.getX(), Gdx.input.getY());
                })));
                armedButton = null;
                handled = true;
            }
            return handled;
        }

        private void updatePosition() {
            position.set(armedButton.getWidth() / 2, armedButton.getHeight() / 2);
            stageToScreenCoordinates(armedButton.localToStageCoordinates(position));
        }

        private boolean isKeyCodeValid(int keyCode) {
            return keyCode == Keys.LEFT || keyCode == Keys.DOWN || keyCode == Keys.RIGHT;
        }
        
    }
    
}
