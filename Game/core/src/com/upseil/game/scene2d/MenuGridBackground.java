package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.upseil.game.Config.MenuConfigValues.CellMoveSpeed;
import static com.upseil.game.Config.MenuConfigValues.CellSize;
import static com.upseil.game.Config.MenuConfigValues.CellSpacing;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalStripeInDelay;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.FadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalScaleInDelay;
import static com.upseil.game.Config.MenuConfigValues.ScaleInDuration;

import com.artemis.World;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.upseil.game.Config.MenuConfig;
import com.upseil.gdx.math.ExtendedRandom;

public class MenuGridBackground extends AbstractGrid {

    private static final Interpolation CellMoveInterpolation = Interpolation.linear;
    private static final Interpolation ScaleInInterpolation = Interpolation.fade;
    
    private final MenuGridBackgroundStyle style;

    public MenuGridBackground(World world, MenuGridBackgroundStyle style, ExtendedRandom random, float worldWidth, float worldHeight) {
        super(world, style, random, (int) Math.ceil(Math.max(worldWidth, worldHeight) / style.paddedCellSize));
        this.style = style;
    }

    public void randomEntrance(float startDelay) {
        switch (random.randomIntExclusive(3)) {
        case 0: stripeInEntrance(startDelay); break;
        case 1: fadeInEntrance(startDelay); break;
        case 2: scaleInEntrance(startDelay); break;
        }
    }

    public void stripeInEntrance(float startDelay) {
        float width = getWorldWidth();
        float duration = width / style.cellMoveSpeed;
        for (int y = 0; y < getGridHeight(); y++) {
            float delay = startDelay + random.randomFloatExclusive(style.maxAdditionalStripeInDelay);
            float translateX = width * (y % 2 == 0 ? 1 : -1);
            for (int x = 0; x < getGridWidth(); x++) {
                CellActor cell = getCell(x, y);
                cell.moveBy(translateX, 0);
                cell.addAction(Actions.delay(delay, Actions.moveBy(-translateX, 0, duration, CellMoveInterpolation)));
            }
        }
    }
    
    public void fadeInEntrance(float startDelay) {
        for (int x = 0; x < getGridHeight(); x++) {
            for (int y = 0; y < getGridWidth(); y++) {
                float delay = startDelay + random.randomFloatExclusive(style.maxAdditionalFadeInDelay);
                CellActor cell = getCell(x, y);
                cell.getColor().a = 0;
                cell.addAction(Actions.delay(delay, fadeIn(style.fadeInDuration)));
            }
        }
    }

    public void scaleInEntrance(float startDelay) {
        for (int x = 0; x < getGridHeight(); x++) {
            for (int y = 0; y < getGridWidth(); y++) {
                float delay = startDelay + random.randomFloatExclusive(style.maxAdditionalScaleInDelay);
                CellActor cell = getCell(x, y);
                cell.setScale(0);
                cell.addAction(Actions.delay(delay, scaleTo(1, 1, style.scaleInDuration, ScaleInInterpolation)));
            }
        }
    }
    
    public static class MenuGridBackgroundStyle extends GridStyle {

        public final float maxAdditionalStripeInDelay;
        
        public final float maxAdditionalFadeInDelay;
        public final float fadeInDuration;
        
        public final float maxAdditionalScaleInDelay;
        public final float scaleInDuration;
        
        public MenuGridBackgroundStyle(MenuConfig config) {
            this(config.getFloat(CellSize), config.getFloat(CellSpacing), config.getFloat(CellMoveSpeed), config.getFloat(MaxAdditionalStripeInDelay),
                    config.getFloat(MaxAdditionalFadeInDelay), config.getFloat(FadeInDuration), config.getFloat(MaxAdditionalScaleInDelay),
                    config.getFloat(ScaleInDuration));
        }
        
        public MenuGridBackgroundStyle(float cellSize, float spacing, float cellMoveSpeed, float maxAdditionalStripeInDelay, float maxAdditionalFadeInDelay,
                float fadeInDuration, float maxAdditionalScaleInDelay, float scaleInDuration) {
            super(cellSize, spacing, cellMoveSpeed);
            this.maxAdditionalStripeInDelay = maxAdditionalStripeInDelay;
            this.maxAdditionalFadeInDelay = maxAdditionalFadeInDelay;
            this.fadeInDuration = fadeInDuration;
            this.maxAdditionalScaleInDelay = maxAdditionalScaleInDelay;
            this.scaleInDuration = scaleInDuration;
        }
        
    }
    
}
