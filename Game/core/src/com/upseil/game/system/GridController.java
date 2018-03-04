package com.upseil.game.system;

import com.artemis.BaseSystem;
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
import com.upseil.game.GameApplication;
import com.upseil.game.GameConfig;
import com.upseil.game.GameConfig.GridConfig;
import com.upseil.game.Layers;
import com.upseil.game.Tag;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Direction;
import com.upseil.game.scene2d.GridActor;
import com.upseil.gdx.artemis.component.ActorComponent;
import com.upseil.gdx.artemis.component.InputHandler;
import com.upseil.gdx.artemis.component.Layer;
import com.upseil.gdx.artemis.component.Scene;
import com.upseil.gdx.artemis.event.ResizeEvent;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.viewport.PaddedScreen;
import com.upseil.gdx.viewport.PartialScalingViewport;
import com.upseil.gdx.viewport.PartialWorldViewport;

public class GridController extends BaseSystem {
    
    private TagManager<Tag> tagManager;
    private LayeredSceneRenderSystem<?> renderSystem;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenPadding;
    private Scene gridScene;
    private float slowMoDistanceThreshold;
    private float minSlowMoTimeScale;
    private float timeScaleAlterationRate;
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
    
    @Override
    protected void initialize() {
        world.getSystem(EventSystem.class).registerHandler(ResizeEvent.Type, e -> updateScreenSize = true);
        
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getGridConfig();
        slowMoDistanceThreshold = (config.getCellSize() + config.getSpacing()) * config.getSlowMoThresholdFactor();
        minSlowMoTimeScale = config.getMinSlowMoTimeScale();
        timeScaleAlterationRate = (1 - minSlowMoTimeScale) * 3;
        loseEpsilon = MathUtils.FLOAT_ROUNDING_ERROR * 100;
        
        float worldSize = config.getGridSize() * (config.getCellSize() + config.getSpacing()) + 2 * config.getBorderSize();
        screenPadding = new PaddedScreen();
        PartialWorldViewport gridViewport = new PartialScalingViewport(screenPadding, Scaling.fit, worldSize, worldSize);
        Stage gridStage = new Stage(gridViewport, world.getSystem(LayeredSceneRenderSystem.class).getGlobalBatch());
        
        Entity gridEntity = world.createEntity();
        EntityEdit gridEdit = gridEntity.edit();
        gridEdit.create(Layer.class).setZIndex(Layers.World.getZIndex());
        gridEdit.create(InputHandler.class).setProcessor(gridStage);
        gridScene = gridEdit.create(Scene.class).initialize(gridStage);

        grid = new GridActor(world, GameApplication.Random, config.getExclusionAreaSize());
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
            GameApplication.HUD.setButtonsDisabled(false);
            grid.reset(config.getExclusionAreaSize());
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
                    GameApplication.HUD.setButtonsDisabled(false);
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
            float timeScale = Interpolation.pow2Out.apply(startTimeScale, targetTimeScale, timeScaleAlpha);
            gridScene.setTimeScale(timeScale);
//            System.out.println(String.format("Target %.2f, Current %.2f, Alpha %.4f", targetTimeScale, timeScale, timeScaleAlpha));
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
    }

    public void updateScreenSize() {
        int padding = config.getGridPadding();
        int top = Math.round(GameApplication.HUD.getTopHeight()) + padding;
        int left = Math.round(GameApplication.HUD.getLeftWidth()) + padding;
        int bottom = Math.round(GameApplication.HUD.getBottomHeight()) + padding;
        int right = Math.round(GameApplication.HUD.getRightWidth()) + padding;
        
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
    
}