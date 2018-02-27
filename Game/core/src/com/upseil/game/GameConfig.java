package com.upseil.game;

import com.upseil.gdx.artemis.config.SaveConfig;
import com.upseil.gdx.config.AbstractConfig;
import com.upseil.gdx.config.RawConfig;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.scene2d.util.BorderBuilder;
import com.upseil.gdx.scene2d.util.DividerBuilder;

public class GameConfig extends AbstractConfig {
    
    private final GridConfig gridConfig;
    private final HUDConfig hudConfig;

    private final SaveConfig saveConfig;
    private final BackgroundBuilder.Config backgroundBuilderConfig;
    private final BorderBuilder.Config borderBuilderConfig;
    private final DividerBuilder.Config dividerBuilderConfig;

    public GameConfig(String path) {
        super(path);
        gridConfig = new GridConfig(getRawConfig().getChild("grid"));
        hudConfig = new HUDConfig(getRawConfig().getChild("HUD"));
        
        saveConfig = new SaveConfig(getRawConfig().getChild("savegame"));
        backgroundBuilderConfig = new BackgroundBuilder.Config(getRawConfig().getChild("backgroundBuilder"));
        borderBuilderConfig = new BorderBuilder.Config(getRawConfig().getChild("borderBuilder"));
        dividerBuilderConfig = new DividerBuilder.Config(getRawConfig().getChild("dividerBuilder"));
    }
    
    public GridConfig getGridConfig() {
        return gridConfig;
    }
    
    public HUDConfig getHUDConfig() {
        return hudConfig;
    }
    
    public SaveConfig getSavegameConfig() {
        return saveConfig;
    }
    
    public BackgroundBuilder.Config getBackgroundBuilderConfig() {
        return backgroundBuilderConfig;
    }
    
    public BorderBuilder.Config getBorderBuilderConfig() {
        return borderBuilderConfig;
    }
    
    public DividerBuilder.Config getDividerBuilderConfig() {
        return dividerBuilderConfig;
    }

    public static class GridConfig extends AbstractConfig {

        public GridConfig(RawConfig config) {
            super(config);
        }
        
        public int getGridSize() {
            return getInt("gridSize");
        }
        
        public float getExclusionAreaSize() {
            return getFloat("exclusionAreaSize");
        }
        
        public int getGridPadding() {
            return getInt("gridPadding");
        }
        
        public float getCellSize() {
            return getFloat("cellSize");
        }
        
        public float getSpacing() {
            return getFloat("spacing");
        }
        
        public float getMaxRemovalDelay() {
            return getFloat("maxRemovalDelay");
        }
        
        public float getRemovalDuration() {
            return getFloat("removalDuration");
        }
        
        public float getRemovalMoveAmount() {
            return getFloat("removalMoveAmount");
        }
        
        public float getRemovalScaleTo() {
            return getFloat("removalScaleTo");
        }
        
        public float getCellMoveSpeed() {
            return getFloat("cellMoveSpeed");
        }
        
        public float getSlowMoThresholdFactor() {
            return getFloat("slowMoThresholdFactor");
        }
        
        public float getMinSlowMoTimeScale() {
            return getFloat("minSlowMoTimeScale");
        }
        
    }

    public static class HUDConfig extends AbstractConfig {

        public HUDConfig(RawConfig config) {
            super(config);
        }
        
        public float getPadding() {
            return getFloat("padding");
        }
        
        public float getUpdateInterval() {
            return getFloat("updateInterval");
        }
        
        public float getButtonRatio() {
            return getFloat("buttonRatio");
        }
        
        public float getCounterSize() {
            return getFloat("counterSize");
        }
        
    }
    
}
