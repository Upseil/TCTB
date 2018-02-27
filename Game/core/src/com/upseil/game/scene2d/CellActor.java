package com.upseil.game.scene2d;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.upseil.game.domain.Color;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;

public class CellActor extends Image {
    
    private Skin skin;
    private Color color;
    
    public CellActor(Skin skin, Color color, float size) {
        super();
        this.skin = skin;
        setCellColor(color);
        setSize(size, size);
        setOrigin(Align.center);
    }
    
    public Color getCellColor() {
        return color;
    }

    public void setCellColor(Color color) {
        this.color = color;
        setDrawable(BackgroundBuilder.byColor(this.skin, color.getName()));
    }
    
    @Override
    public String toString() {
        return super.toString() + "[" + color + "]";
    }
    
}
