package com.upseil.game.scene2d;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pool;
import com.upseil.game.domain.Color;
import com.upseil.gdx.pool.Pooled;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;

public class CellActor extends Image implements Pooled<CellActor> {
    
    private Pool<CellActor> pool;
    private Color color;
    
    public CellActor initialize(Skin skin, Color color, float size) {
        setDrawable(BackgroundBuilder.byColor(skin, "white"));
        setCellColor(color);
        setSize(size, size);
        setOrigin(Align.center);
        return this;
    }
    
    public Color getCellColor() {
        return color;
    }

    public void setCellColor(Color color) {
        this.color = color;
        setColor(Colors.get(color.getName()));
    }
    
    @Override
    public boolean remove() {
        boolean removed = super.remove();
        if (removed) {
            free();
        }
        return removed;
    }

    @Override
    public Pool<CellActor> getPool() {
        return pool;
    }

    @Override
    public void setPool(Pool<CellActor> pool) {
        this.pool = pool;
    }

    @Override
    public void free() {
        if (pool != null) {
            pool.free(this);
        }
    }

    @Override
    public void reset() {
        pool = null;
        color = null;
        clear();
        setDrawable(null);
        setSize(0, 0);
        setOrigin(Align.bottomLeft);
        setPosition(0, 0);
        setColor(com.badlogic.gdx.graphics.Color.WHITE);
        setScale(1);
    }
    
    @Override
    public String toString() {
        return super.toString() + "[" + color + "]";
    }
    
}
