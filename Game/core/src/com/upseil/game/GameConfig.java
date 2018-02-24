package com.upseil.game;

import com.upseil.gdx.artemis.config.SaveConfig;
import com.upseil.gdx.config.AbstractConfig;
import com.upseil.gdx.config.RawConfig;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.scene2d.util.BorderBuilder;
import com.upseil.gdx.scene2d.util.DividerBuilder;

public class GameConfig extends AbstractConfig {
    
    private final HUDConfig hudConfig;

    private final SaveConfig saveConfig;
    private final BackgroundBuilder.Config backgroundBuilderConfig;
    private final BorderBuilder.Config borderBuilderConfig;
    private final DividerBuilder.Config dividerBuilderConfig;

    public GameConfig(String path) {
        super(path);
        hudConfig = new HUDConfig(getRawConfig().getChild("HUD"));
        
        saveConfig = new SaveConfig(getRawConfig().getChild("savegame"));
        backgroundBuilderConfig = new BackgroundBuilder.Config(getRawConfig().getChild("backgroundBuilder"));
        borderBuilderConfig = new BorderBuilder.Config(getRawConfig().getChild("borderBuilder"));
        dividerBuilderConfig = new DividerBuilder.Config(getRawConfig().getChild("dividerBuilder"));
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
        
        public float getButtonSize() {
            return getFloat("buttonSize");
        }
        
        public float getButtonLength() {
            return getFloat("buttonLength");
        }
        
        public float getCounterSize() {
            return getFloat("counterSize");
        }
        
    }
    
}
