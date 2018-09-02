package com.upseil.game.scene2d;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.ColorAction;
import com.upseil.game.domain.Color;

public class CellColorAction extends ColorAction {
    
    private Color cellColor;
    
    public CellColorAction initialize(Color cellColor, float duration, Interpolation interpolation) {
        setCellColor(cellColor);
        setDuration(duration);
        setInterpolation(interpolation);
        return this;
    }
    
    @Override
    public void setTarget(Actor target) {
        if (!(target instanceof CellActor)) {
            throw new IllegalArgumentException("Targets of this action must be of type " + CellActor.class.getName());
        }
        super.setTarget(target);
    }
    
    @Override
    protected void end() {
        ((CellActor) target).setCellColor(cellColor);
    }

    public Color getCellColor() {
        return cellColor;
    }

    public void setCellColor(Color cellColor) {
        this.cellColor = cellColor;
        setEndColor(Colors.get(cellColor.getName()));
    }
    
}
