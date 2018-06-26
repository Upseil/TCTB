package com.upseil.game.system;

import static com.upseil.game.Config.GridConfigValues.*;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.upseil.game.Config.GameConfig;
import com.upseil.game.Config.GridConfig;
import com.upseil.game.GameApplication;
import com.upseil.game.Layers;
import com.upseil.game.Tag;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Direction;
import com.upseil.game.scene2d.GridActor;
import com.upseil.game.scene2d.HUDStage;
import com.upseil.gdx.artemis.component.ActorComponent;
import com.upseil.gdx.artemis.component.Ignore;
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.event.ResizeEvent;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.math.BuiltInInterpolation;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialWorldViewport;

public class GridController extends BaseSystem {
    
    private TagManager<Tag> tagManager;
    private LayeredSceneRenderSystem<?> renderSystem;
    private ComponentMapper<Scene> sceneMapper;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenPadding;
    private Scene gridScene;
    private float slowMoDistanceThreshold;
    private float minSlowMoTimeScale;
    private float timeScaleAlterationRate;
    private Interpolation timeScaleIncreaseInterpolation;
    private Interpolation timeScaleDecreaseInterpolation;
    private float loseEpsilon;
    
    private GridActor grid;
    
    private boolean updateScreenSize;
    private boolean resetGrid;
    private boolean lost;
    private float grayness;
    private Color colorToRemove;
    private Direction fillDirection;
    private float blackWhiteDistance;

    private float startTimeScale;
    private float targetTimeScale;
    private float timeScaleAlterationDuration;
    private float timeScaleAlterationTime;
    private Interpolation timeScaleInterpolation;
    
    @Override
    protected void initialize() {
        world.getSystem(EventSystem.class).registerHandler(ResizeEvent.Type, e -> updateScreenSize = true);
        
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getGridConfig();
        slowMoDistanceThreshold = (config.getFloat(CellSize) + config.getFloat(Spacing)) * config.getFloat(SlowMoThresholdFactor);
        minSlowMoTimeScale = config.getFloat(MinSlowMoTimeScale);
        timeScaleAlterationRate = (1 - minSlowMoTimeScale) * config.getFloat(TimeScaleAlterationRate);
        timeScaleIncreaseInterpolation = config.getEnum(TimeScaleIncreaseInterpolation, BuiltInInterpolation.class).get();
        timeScaleDecreaseInterpolation = config.getEnum(TimeScaleDecreaseInterpolation, BuiltInInterpolation.class).get();
        loseEpsilon = 0.1f;
        
        float worldSize = config.getFloat(GridSize) * (config.getFloat(CellSize) + config.getFloat(Spacing)) + 2 * config.getFloat(BorderSize);
        screenPadding = new PaddedScreen();
        PartialWorldViewport gridViewport = new PartialScalingViewport(screenPadding, Scaling.fit, worldSize, worldSize);
        Stage gridStage = new Stage(gridViewport, world.getSystem(LayeredSceneRenderSystem.class).getGlobalBatch());
        
        Entity gridEntity = world.createEntity();
        EntityEdit gridEdit = gridEntity.edit();
        gridEdit.create(Ignore.class);
        gridEdit.create(Layer.class).setZIndex(Layers.World.getZIndex());
        gridEdit.create(InputHandler.class).setProcessor(gridStage);
        gridScene = gridEdit.create(Scene.class).initialize(gridStage);

        grid = new GridActor(world, GameApplication.Random, config.getFloat(ExclusionAreaSize));
        gridEdit.create(ActorComponent.class).set(grid);
        gridScene.addActor(grid);

        tagManager.register(Tag.Grid, gridEntity);
        
        resetGrid = false;
        lost = false;
        grayness = 0;
        colorToRemove = null;
        fillDirection = null;
        blackWhiteDistance = -1;
        startTimeScale = 1;
        targetTimeScale = 1;
        timeScaleAlterationDuration = 0;
        timeScaleAlterationTime = 0;
    }

    public int getExpectedColorCount() {
        int expectedColorCount = (grid.getGridWidth() * grid.getGridHeight()) / Color.size();
        return expectedColorCount;
    }
    
    @Override
    protected void processSystem() {
        if (updateScreenSize) {
            updateScreenSize();
            updateScreenSize = false;
        }
        
        if (resetGrid) {
            setTimeScale(1);
            getHUD().setButtonsDisabled(false);
            grid.reset(config.getFloat(ExclusionAreaSize));
            resetGrid = false;
        }
        
        if (lost && grayness < 1) {
            grayness += world.delta;
            ShaderProgram shader = renderSystem.getGlobalBatch().getShader(); 
            int attribute = shader.getAttributeLocation("a_grayness");
            Gdx.gl20.glVertexAttrib1f(attribute, Math.min(grayness, 1));
        }
        
        if (blackWhiteDistance >= 0) {
            checkBlackWhiteDistance();
            if (!grid.isMovementInProgress()) {
                if (!lost) {
                    grid.randomizeBorderColors();
                    getHUD().setButtonsDisabled(false);
                }
                setTimeScale(1);
                blackWhiteDistance = -1;
            }
        }
        
        if (fillDirection != null && !grid.isRemovalInProgress()) {
            grid.fillGrid(fillDirection);
            fillDirection = null;
            blackWhiteDistance = grid.getMinBlackWhiteDistance();
        }
        
        if (colorToRemove != null) {
            grid.removeCells(colorToRemove);
            fillDirection = fromColor(colorToRemove);
            colorToRemove = null;
        }
        
        if (timeScaleAlterationTime < timeScaleAlterationDuration) {
            timeScaleAlterationTime += world.delta;
            float timeScaleAlpha = MathUtils.clamp(timeScaleAlterationTime / timeScaleAlterationDuration, 0, 1);
            float timeScale = timeScaleInterpolation.apply(startTimeScale, targetTimeScale, timeScaleAlpha);
            gridScene.setTimeScale(timeScale);
        }
    }

    private Direction fromColor(Color color) {
        switch (color) {
        case Color0:
            return Direction.Left;
        case Color1:
            return Direction.Bottom;
        case Color2:
            return Direction.Right;
        case Empty:
        case Black:
        case White:
            break;
        }
        throw new IllegalArgumentException("No " + Direction.class.getSimpleName() + " specified for " +
                                           Color.class.getSimpleName() + " " + color);
    }

    private void checkBlackWhiteDistance() {
        float newBlackWhiteDistance = grid.getMinBlackWhiteDistance();
        if (newBlackWhiteDistance >= blackWhiteDistance) {
            setTargetTimeScale(1);
            return;
        }
        
        blackWhiteDistance = newBlackWhiteDistance;
        setTargetTimeScale(blackWhiteDistance / slowMoDistanceThreshold);
        // TODO This is not robust against big delta times
         if (MathUtils.isZero(blackWhiteDistance, loseEpsilon)) {
            // TODO Proper state flow
             grid.abortMovement();
             lost = true;
            
//            resetGrid = true;
//            gameState.setScore(0);
//            GameApplication.HUD.setUpdateValueLabels(true);
        }
    }
    
    private void setTimeScale(float timeScale) {
        gridScene.setTimeScale(timeScale);
        timeScaleAlterationDuration = 0;
        timeScaleAlterationTime = 0;
    }
    
    private void setTargetTimeScale(float timeScale) {
        startTimeScale = gridScene.getTimeScale();
        targetTimeScale = MathUtils.clamp(timeScale, minSlowMoTimeScale, 1);
        timeScaleAlterationDuration = Math.abs(startTimeScale - targetTimeScale) / timeScaleAlterationRate;
        timeScaleAlterationTime = 0;
        timeScaleInterpolation = startTimeScale < targetTimeScale ? timeScaleIncreaseInterpolation
                                                                  : timeScaleDecreaseInterpolation;
    }

    public void updateScreenSize() {
        HUDStage hud = getHUD();
        
        int padding = config.getInt(GridPadding);
        int top = Math.round(hud.getTopHeight()) + padding;
        int left = Math.round(hud.getLeftWidth()) + padding;
        int bottom = Math.round(hud.getBottomHeight()) + padding;
        int right = Math.round(hud.getRightWidth()) + padding;
        
        screenPadding.pad(top, left, bottom, right);
        gridScene.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
    
    public int getColorCount(int colorNumber) {
        return grid.getColorCount(colorNumber);
    }

    public void select(Color color) {
        if (color == null) {
            deselectAll();
        } else {
            getSpriteDrawable(color).getSprite().setColor(skin.getColor(color.getName() + "-emphasize"));
        }
    }

    private void deselectAll() {
        for (int number = 0; number < Color.size(); number++) {
            getSpriteDrawable(Color.forNumber(number)).getSprite().setColor(skin.getColor(Color.forNumber(number).getName()));
        }
    }

    private SpriteDrawable getSpriteDrawable(Color color) {
        String colorName = color.getName();
        SpriteDrawable spriteDrawable = skin.optional(colorName, SpriteDrawable.class);
        if (spriteDrawable == null) {
            spriteDrawable = (SpriteDrawable) BackgroundBuilder.byColor(skin, colorName);
            skin.add(colorName, spriteDrawable, SpriteDrawable.class);
        }
        return spriteDrawable;
    }

    public void remove(Color color) {
        colorToRemove = color;
    }
    
    private HUDStage getHUD() {
        return (HUDStage) sceneMapper.get(tagManager.getEntityId(Tag.HUD)).getStage();
    }
    
}