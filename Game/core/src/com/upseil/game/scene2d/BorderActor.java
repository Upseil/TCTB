package com.upseil.game.scene2d;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Direction;
import com.upseil.gdx.scene2d.PolygonActor;

public class BorderActor extends PolygonActor {
    
    private final Skin skin;
    private final Direction direction;
    private Color color;
    
    public BorderActor(Skin skin, Direction direction, float worldWidth, float worldHeight, float borderSize) {
        super(getVertices(direction, worldWidth, worldHeight, borderSize));
        this.skin = skin;
        this.direction = direction;
        switch (direction) {
        case Top:
            setPosition(0, worldHeight - borderSize);
            break;
        case Left:
        case Bottom:
            setPosition(0, 0);
            break;
        case Right:
            setPosition(worldWidth - borderSize, 0);
            break;
        }
    }

    private static float[] getVertices(Direction direction, float worldWidth, float worldHeight, float borderSize) {
        switch (direction) {
        case Top:
            return new float[] { 0,worldHeight,  borderSize,worldHeight-borderSize,  worldWidth-borderSize,worldHeight-borderSize,  worldWidth,worldHeight };
        case Left:
            return new float[] { 0,worldHeight,  0,0,  borderSize,borderSize,  borderSize,worldHeight-borderSize };
        case Bottom:
            return new float[] { borderSize,borderSize,  0,0,  worldWidth,0,  worldWidth-borderSize,borderSize };
        case Right:
            return new float[] { worldWidth-borderSize,worldHeight-borderSize,  worldWidth-borderSize,borderSize,  worldWidth,0,  worldWidth,worldHeight };
        }
        throw new IllegalArgumentException("Can't create vertices for direction " + direction);
    }
    
    public Direction getDirection() {
        return direction;
    }
    
    public Color getBorderColor() {
        return color;
    }
    
    public void setBorderColor(Color color) {
        if (this.color != color) {
            this.color = color;
            setColor(skin.getColor(color.getName()));
        }
    }
    
}
