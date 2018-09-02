package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.action;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.repeat;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import static com.upseil.game.Config.MenuConfigValues.CellMoveSpeed;
import static com.upseil.game.Config.MenuConfigValues.CellSize;
import static com.upseil.game.Config.MenuConfigValues.CellSpacing;
import static com.upseil.game.Config.MenuConfigValues.FadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.GridActionDuration;
import static com.upseil.game.Config.MenuConfigValues.GridActionInterval;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalActionDelay;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalScaleInDelay;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalStripeInDelay;
import static com.upseil.game.Config.MenuConfigValues.MaxConcurrentActions;
import static com.upseil.game.Config.MenuConfigValues.RumbleDistance;
import static com.upseil.game.Config.MenuConfigValues.RumblesPerAction;
import static com.upseil.game.Config.MenuConfigValues.ScaleInDuration;
import static com.upseil.game.Config.MenuConfigValues.ShakeRotation;
import static com.upseil.game.Config.MenuConfigValues.ShakesPerAction;
import static com.upseil.game.Config.MenuConfigValues.ShiftLineChance;
import static com.upseil.game.Config.MenuConfigValues.SpinsPerAction;

import java.util.function.Supplier;

import com.artemis.World;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.upseil.game.Config.MenuConfig;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Direction;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.util.function.FloatConsumer;

public class MenuGridBackground extends AbstractGrid {

    private static final int FindRandomCellTries = 25;
    private static final Vector2 Vector = new Vector2();
    
    private final MenuGridBackgroundStyle style;
    
    private final FloatConsumer[] entrances;
    private final Supplier<Action>[] actionSuppliers;
    
    private float entranceDuration;
    private float actionAccumulator;

    @SuppressWarnings("unchecked")
    public MenuGridBackground(World world, MenuGridBackgroundStyle style, ExtendedRandom random, int size) {
        super(world, style, random, size);
        this.style = style;
        actionAccumulator = style.actionInterval * 0.8f;
        
        entrances = new FloatConsumer[3];
        entrances[0] = this::stripeInEntrance;
        entrances[1] = this::fadeInEntrance;
        entrances[2] = this::scaleInEntrance;
        
        actionSuppliers = new Supplier[4];
        actionSuppliers[0] = this::createHideAction;
        actionSuppliers[1] = this::createShakeAction;
        actionSuppliers[2] = this::createRumbleAction;
        actionSuppliers[3] = this::createSpinAction;
    }
    
    @Override
    public void act(float delta) {
        super.act(delta);
        if (entranceDuration > 0) {
            entranceDuration -= delta;
        }
        if (entranceDuration <= 0) {
            actionAccumulator += delta;
            if (actionAccumulator >= style.actionInterval) {
                actionAccumulator -= style.actionInterval;
                randomAction();
            }
        }
    }

    public void randomEntrance(float startDelay) {
        entrances[random.randomIntExclusive(entrances.length)].accept(startDelay);
    }

    public void stripeInEntrance(float startDelay) {
        float width = getWorldWidth();
        float duration = width / style.cellMoveSpeed;
        for (int y = 0; y < getGridHeight(); y++) {
            float delay = startDelay + random.randomFloatExclusive(style.maxAdditionalStripeInDelay);
            updateEntranceDuration(delay + duration);
            
            float translateX = width * (y % 2 == 0 ? 1 : -1);
            for (int x = 0; x < getGridWidth(); x++) {
                CellActor cell = getCell(x, y);
                cell.moveBy(translateX, 0);
                cell.addAction(Actions.delay(delay, Actions.moveBy(-translateX, 0, duration, Interpolation.linear)));
            }
        }
    }
    
    public void fadeInEntrance(float startDelay) {
        for (int x = 0; x < getGridHeight(); x++) {
            for (int y = 0; y < getGridWidth(); y++) {
                float delay = startDelay + random.randomFloatExclusive(style.maxAdditionalFadeInDelay);
                updateEntranceDuration(delay + style.fadeInDuration);
                
                CellActor cell = getCell(x, y);
                cell.getColor().a = 0;
                cell.addAction(Actions.delay(delay, fadeIn(style.fadeInDuration, Interpolation.fade)));
            }
        }
    }

    public void scaleInEntrance(float startDelay) {
        for (int x = 0; x < getGridHeight(); x++) {
            for (int y = 0; y < getGridWidth(); y++) {
                float delay = startDelay + random.randomFloatExclusive(style.maxAdditionalScaleInDelay);
                updateEntranceDuration(delay + style.scaleInDuration);
                
                CellActor cell = getCell(x, y);
                cell.setScale(0);
                cell.addAction(Actions.delay(delay, scaleTo(1, 1, style.scaleInDuration, Interpolation.fade)));
            }
        }
    }
    
    private void updateEntranceDuration(float entranceDuration) {
        if (entranceDuration > this.entranceDuration) {
            this.entranceDuration = entranceDuration;
        }
    }
    
    private void randomAction() {
        randomSimpleActions();
        
        if (random.randomBoolean(style.shiftLineChance)) {
            Direction direction = Direction.random(random);
            int number = direction.isHorizontal() ? random.randomIntExclusive(getGridHeight()) : random.randomIntExclusive(getGridWidth());
            shiftLine(number, direction);
        }
    }
    
    private void randomSimpleActions() {
        int newActions = random.randomIntExclusive(style.maxConcurrentActions);
        while (newActions >= 0) {
            CellActor cell = randomInactiveCell(FindRandomCellTries);
            if (cell != null) {
                Color newColor = null;
                do {
                    newColor = Color.random(random);
                } while (newColor == cell.getCellColor());
                cell.addAction(delay(random.randomFloatExclusive(style.maxAdditionalActionDelay),
                        parallel(actionSuppliers[random.randomIntExclusive(actionSuppliers.length)].get(),
                                action(CellColorAction.class).initialize(newColor, style.actionDuration, Interpolation.fade))
                        ));
            }
            newActions--;
        }
    }
    
    private CellActor randomInactiveCell(int tries) {
        CellActor cell = null;
        int triesLeft = tries;
        do {
            int x = random.randomIntExclusive(getGridWidth());
            int y = random.randomIntExclusive(getGridHeight());
            cell = getCell(x, y);
            triesLeft--;
        } while (cell.getActions().size > 0 && triesLeft > 0);
        return cell;
    }
    
    private void shiftLine(int number, Direction direction) {
        float moveByX = style.paddedCellSize * direction.getDeltaX();
        float moveByY = style.paddedCellSize * direction.getDeltaY();
        
        int x = direction ==   Direction.Left ? 0 : direction == Direction.Right ? getGridWidth() - 1  : number;
        int y = direction == Direction.Bottom ? 0 : direction ==   Direction.Top ? getGridHeight() - 1 : number;
        for (int i = direction.isHorizontal() ? getGridWidth() : getGridHeight(); i >= 0; i--) {
            CellActor cell;
            if (isInsideGrid(x, y)) {
                cell = getCell(x, y);
            } else {
                cell = createCell(x, y, Color.random(random));
            }
            
            Action action = Actions.moveBy(moveByX, moveByY, style.actionDuration, Interpolation.linear);
            int newX = x + direction.getDeltaX();
            int newY = y + direction.getDeltaY();
            if (isInsideGrid(newX, newY)) {
                setCell(newX, newY, cell);
            } else {
                action = sequence(action, Actions.removeActor());
            }
            cell.addAction(action);
            
            x -= direction.getDeltaX();
            y -= direction.getDeltaY();
        }
    }

    private Action createHideAction() {
        float frameDuration = style.actionDuration / 3;
        Interpolation interpolation = Interpolation.fade;
        return sequence(scaleTo(0, 0, frameDuration, interpolation), delay(frameDuration), scaleTo(1, 1, frameDuration, interpolation));
    }

    private Action createShakeAction() {
        float shakeDuration = style.actionDuration / style.shakesPerAction;
        float frameDuration = shakeDuration / 4;
        Interpolation interpolation = Interpolation.linear;
        Action singleShake = sequence(Actions.rotateBy(-style.shakeRotation, frameDuration, interpolation),
                                      Actions.rotateBy(2 * style.shakeRotation, frameDuration, interpolation),
                                      Actions.rotateBy(-style.shakeRotation, frameDuration, interpolation));
        return repeat(style.shakesPerAction, singleShake);
    }

    private Action createRumbleAction() {
        float rumbleDuration = style.actionDuration / style.rumblesPerAction;
        float frameDuration = rumbleDuration / 2;
        Interpolation interpolation = Interpolation.linear;
        SequenceAction action = sequence();
        for (int i = 0; i < style.rumblesPerAction; i++) {
            Vector.set(style.rumbleDistance, 0).rotate(random.randomIntExclusive(360));
            action.addAction(Actions.moveBy(Vector.x, Vector.y, frameDuration, interpolation));
            action.addAction(Actions.moveBy(-Vector.x, -Vector.y, frameDuration, interpolation));
        }
        return action;
    }
    
    private Action createSpinAction() {
        return Actions.rotateBy(style.spinsPerAction * 360, style.actionDuration, Interpolation.swing);
    }
    
    public static class MenuGridBackgroundStyle extends GridStyle {

        // Entrance Animation
        public final float maxAdditionalStripeInDelay;
        
        public final float maxAdditionalFadeInDelay;
        public final float fadeInDuration;
        
        public final float maxAdditionalScaleInDelay;
        public final float scaleInDuration;
        
        // Action Animation
        public final float actionInterval;
        public final float shiftLineChance;
        public final int maxConcurrentActions;
        public final float maxAdditionalActionDelay;
        public final float actionDuration;
        
        public final int shakesPerAction;
        public final float shakeRotation;

        public final int rumblesPerAction;
        public final float rumbleDistance;

        public final float spinsPerAction;
        
        public MenuGridBackgroundStyle(MenuConfig config) {
            this(config.getFloat(CellSize), config.getFloat(CellSpacing), config.getFloat(CellMoveSpeed), config.getFloat(MaxAdditionalStripeInDelay),
                    config.getFloat(MaxAdditionalFadeInDelay), config.getFloat(FadeInDuration), config.getFloat(MaxAdditionalScaleInDelay),
                    config.getFloat(ScaleInDuration), config.getFloat(GridActionInterval), config.getFloat(ShiftLineChance),
                    config.getInt(MaxConcurrentActions), config.getFloat(MaxAdditionalActionDelay), config.getFloat(GridActionDuration),
                    config.getInt(ShakesPerAction), config.getFloat(ShakeRotation), config.getInt(RumblesPerAction), config.getFloat(RumbleDistance),
                    config.getFloat(SpinsPerAction));
        }
        
        public MenuGridBackgroundStyle(float cellSize, float spacing, float cellMoveSpeed, float maxAdditionalStripeInDelay, float maxAdditionalFadeInDelay,
                float fadeInDuration, float maxAdditionalScaleInDelay, float scaleInDuration, float actionInterval, float shiftLineChance,
                int maxConcurrentActions, float maxAdditionalActionDelay, float actionDuration, int shakesPerAction, float shakeRotation, int rumblesPerAction,
                float rumbleDistance, float spinsPerAction) {
            super(cellSize, spacing, cellMoveSpeed);
            this.maxAdditionalStripeInDelay = maxAdditionalStripeInDelay;
            this.maxAdditionalFadeInDelay = maxAdditionalFadeInDelay;
            this.fadeInDuration = fadeInDuration;
            this.maxAdditionalScaleInDelay = maxAdditionalScaleInDelay;
            this.scaleInDuration = scaleInDuration;
            this.actionInterval = actionInterval;
            this.shiftLineChance = shiftLineChance;
            this.maxConcurrentActions = maxConcurrentActions;
            this.maxAdditionalActionDelay = maxAdditionalActionDelay;
            this.actionDuration = actionDuration;
            this.shakesPerAction = shakesPerAction;
            this.shakeRotation = shakeRotation;
            this.rumblesPerAction = rumblesPerAction;
            this.rumbleDistance = rumbleDistance;
            this.spinsPerAction = spinsPerAction;
        }
        
    }
    
}
