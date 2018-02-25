package com.upseil.game.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.upseil.game.domain.Color;
import com.upseil.game.system.GridController;

public class ButtonListener extends ClickListener {

    private final HUDStage stage;
    private final GridController gridController;
    
    private final Color color;
    private final Button button;
    
    public ButtonListener(HUDStage stage, GridController gridController, Color color, Button button) {
        this.stage = stage;
        this.gridController = gridController;
        this.color = color;
        this.button = button;
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        super.enter(event, x, y, pointer, fromActor);
        
        if (!button.isDisabled()) {
            stage.setUpdateValueLabels(true);
            gridController.select(color);
        }
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
