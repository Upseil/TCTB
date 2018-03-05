package com.upseil.game.scene2d;

import com.artemis.World;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.upseil.game.domain.Color;
import com.upseil.game.system.GridController;
import com.upseil.gdx.artemis.event.ResizeEvent;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.scene2d.SimpleKeyPressListener;

@Wire
public class ButtonListener extends ClickListener {
    
    private static final Vector2 tmpVector = new Vector2();

    private GridController gridController;
    
    private final HUDStage stage;
    
    private final Color color;
    private final Button button;
    private final int keyCode;
    
    private final Vector2 buttonStagePosition;
    private boolean stagePositionInvalid;
    private boolean keyUpEnabled;
    
    public ButtonListener(HUDStage stage, World world, Color color, Button button, int keyCode) {
        world.inject(this);
        this.stage = stage;
        this.color = color;
        this.button = button;
        this.keyCode = keyCode;
        
        buttonStagePosition = new Vector2();
        stagePositionInvalid = true;
        keyUpEnabled = false;
        
        EventSystem eventSystem = world.getSystem(EventSystem.class);
        eventSystem.registerHandler(ResizeEvent.Type, e -> stagePositionInvalid = true);
        stage.addListener(new SimpleKeyPressListener(this::keyDown, this::keyUp));
    }
    
    private boolean keyDown(InputEvent event) {
        boolean handled = false;
        int keyCode = event.getKeyCode();
        if (keyCode == Keys.ESCAPE && keyUpEnabled) {
            // Aborting the click simulation
            stage.mouseMoved(Gdx.input.getX(), Gdx.input.getY());
            keyUpEnabled = false;
            handled = true;
        }
        if (this.keyCode == keyCode) {
            // Simulating a left mouse button over the button
            tmpVector.set(getButtonStagePosition());
            stage.stageToScreenCoordinates(tmpVector);
            stage.mouseMoved((int) tmpVector.x, (int) tmpVector.y);
            keyUpEnabled = true;
            handled = true;
        }
        return handled;
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        super.enter(event, x, y, pointer, fromActor);
        
        if (!button.isDisabled()) {
            stage.setUpdateValueLabels(true);
            gridController.select(color);
        }
    }
    
    private boolean keyUp(InputEvent event) {
        boolean handled = false;
        if (keyUpEnabled && keyCode == event.getKeyCode()) {
            tmpVector.set(getButtonStagePosition());
            stage.stageToScreenCoordinates(tmpVector);
            stage.touchDown((int) tmpVector.x, (int) tmpVector.y, 0, Buttons.LEFT);
            stage.touchUp((int) tmpVector.x, (int) tmpVector.y, 0, Buttons.LEFT);
            stage.mouseMoved(Gdx.input.getX(), Gdx.input.getY());
            keyUpEnabled = false;
            handled = true;
        }
        return handled;
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        super.exit(event, x, y, pointer, toActor);
        
        if (!isOver() && !button.isDisabled()) {
            stage.setUpdateValueLabels(true);
            gridController.select(null);
        }
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        if (button.isDisabled()) return;
        
        stage.setButtonsDisabled(true);
        stage.setUpdateValueLabels(true);
        gridController.select(null);
        gridController.remove(color);
    }
    
    private Vector2 getButtonStagePosition() {
        if (stagePositionInvalid) {
            buttonStagePosition.set(button.getX(Align.center), button.getY(Align.center));
            button.localToStageCoordinates(buttonStagePosition);
        }
        return buttonStagePosition;
    }
    
}
