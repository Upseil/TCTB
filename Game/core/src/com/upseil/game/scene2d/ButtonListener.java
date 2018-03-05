package com.upseil.game.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.upseil.game.domain.Color;
import com.upseil.game.system.GridController;
import com.upseil.gdx.scene2d.SimpleKeyPressListener;

public class ButtonListener extends ClickListener {
    
    private static final Vector2 tmpVector = new Vector2();

    private final HUDStage stage;
    private final GridController gridController;
    
    private final Color color;
    private final Button button;
    private final int keyCode;
    
    public ButtonListener(HUDStage stage, GridController gridController, Color color, Button button, int keyCode) {
        this.stage = stage;
        this.gridController = gridController;
        this.color = color;
        this.button = button;
        this.keyCode = keyCode;
        
        stage.addListener(new SimpleKeyPressListener(this::keyDown, this::keyUp));
    }
    
    private boolean keyDown(InputEvent event) {
        boolean handled = false;
        int keyCode = event.getKeyCode();
        if (keyCode == Keys.ESCAPE && button.isOver()) {
            // Aborting the click simulation
            stage.mouseMoved(Gdx.input.getX(), Gdx.input.getY());
            handled = true;
        }
        if (this.keyCode == keyCode) {
            // Simulating a left mouse button over the button
            tmpVector.set(button.getX(Align.center), button.getY(Align.center));
            button.localToStageCoordinates(tmpVector);
            stage.stageToScreenCoordinates(tmpVector);
            stage.mouseMoved((int) tmpVector.x, (int) tmpVector.y);
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
        // TODO
        return false;
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
    
}
