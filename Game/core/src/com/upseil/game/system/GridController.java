package com.upseil.game.system;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntityEdit;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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
import com.upseil.game.component.GameState;
import com.upseil.game.domain.Color;
import com.upseil.game.domain.Direction;
import com.upseil.game.scene2d.GridActor;
import com.upseil.game.scene2d.GridActor.GridStyle;
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
    private ComponentMapper<GameState> gameStateMapper;

    @Wire(name="Skin") private Skin skin;
    private GridConfig config;
    
    private PaddedScreen screenPadding;
    private Scene gridScene;
    private float slowMoThresholdFactor;
    private float minSlowMoTimeScale;
    
    private GameState gameState;
    private GridActor grid;
    
    private boolean updateScreenSize;
    private boolean resetGrid;
    private boolean lost;
    private float grayness;
    private Color colorToRemove;
    private Direction fillDirection;
    private float blackWhiteDistance;
    
    @Override
    protected void initialize() {
        world.getSystem(EventSystem.class).registerHandler(ResizeEvent.Type, e -> updateScreenSize = true);
        
        GameConfig gameConfig = world.getRegistered("Config");
        config = gameConfig.getGridConfig();
        slowMoThresholdFactor = config.getSlowMoThresholdFactor();
        minSlowMoTimeScale = config.getMinSlowMoTimeScale();
        
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
    }

    public int getExpectedColorCount() {
        int expectedColorCount = (grid.getGridWidth() * grid.getGridHeight()) / Color.size();
        return expectedColorCount;
    }
    
    @Override
    protected void begin() {
        gameState = gameStateMapper.get(tagManager.getEntityId(Tag.GameState));
    }
    
    @Override
    protected void processSystem() {
        if (updateScreenSize) {
            updateScreenSize();
            updateScreenSize = false;
        }
        
        if (resetGrid) {
            gridScene.setTimeScale(1);
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
            checkBlackAndWhiteCells();
            if (!grid.isMovementInProgress() && !lost) {
                gridScene.setTimeScale(1);
                grid.randomizeBorderColors();
                GameApplication.HUD.setButtonsDisabled(false);
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

    // TODO Interpolate sudden changes of the time scale (black and white cell stop moving or are close together and start moving)
    // TODO Apply time scaling only if it's relevant (e.g. distance gets smaller)
    private void checkBlackAndWhiteCells() {
        // TODO Check if the minimum distance changed
        GridStyle gridStyle = grid.getStyle();
//        float slowMoThreshold = gridStyle.paddedCellSize * slowMoThresholdFactor;
        // TODO How to get the entity that is nearest?
        float loseEpsilon = gridStyle.cellSize / 100;
        
        float minBlackWhiteDistance = grid.getMinBlackWhiteDistance();
        // TODO This is not robust against big delta times
         if (MathUtils.isZero(minBlackWhiteDistance, loseEpsilon)) {
            // TODO Proper state flow
//                gridScene.setPaused(true);
             grid.abortMovement();
             lost = true;
            
//            resetGrid = true;
//            gameState.setScore(0);
//            GameApplication.HUD.setUpdateValueLabels(true);
        }
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