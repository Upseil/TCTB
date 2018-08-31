package com.upseil.game;

import com.badlogic.gdx.utils.JsonValue;
import com.upseil.gdx.artemis.ArtemisConfigs.SaveConfig;
import com.upseil.gdx.properties.EnumerizedJsonBasedProperties;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.scene2d.util.BorderBuilder;
import com.upseil.gdx.scene2d.util.DividerBuilder;
import com.upseil.gdx.util.GDXUtil;

public final class Config {
    
    public enum MenuConfigValues {
        TitleAnimation,
        // Background Grid
        CellSize, CellSpacing, CellMoveSpeed,
            // Entrance Animation
            MaxAdditionalStripeInDelay, MaxAdditionalFadeInDelay, FadeInDuration, MaxAdditionalScaleInDelay, ScaleInDuration,
        GlassAlpha
    }
    
    public enum HUDConfigValues {
        Padding, UpdateInterval, ButtonRatio, ButtonSpacing, CounterSize
    }
    
    public enum GridConfigValues {
        // Grid Structure
        GridSize, ExclusionAreaSize,
        // Grid Layout
        GridPadding, BorderSize, CellSize, Spacing,
        // Grid Removal/Movement
        MaxRemovalDelay, RemovalDuration, RemovalMoveAmount, RemovalScaleTo,
        CellMoveSpeed, TeleportMoveSpeed, TeleportDelay,
        // Time Scaling
        SlowMoThresholdFactor, MinSlowMoTimeScale, TimeScaleAlterationRate,
        TimeScaleIncreaseInterpolation, TimeScaleDecreaseInterpolation
    }
    
    public static class GameConfig {
        
        private final MenuConfig menuConfig;
        private final HUDConfig hudConfig;
        private final GridConfig gridConfig;
        
        private final SaveConfig saveConfig;
        private final BackgroundBuilder.Config backgroundBuilderConfig;
        private final BorderBuilder.Config borderBuilderConfig;
        private final DividerBuilder.Config dividerBuilderConfig;
        
        public GameConfig(String path) {
            JsonValue json = GDXUtil.readJson(path);
            menuConfig = new MenuConfig(json.get("menu"));
            hudConfig = new HUDConfig(json.get("HUD"));
            gridConfig = new GridConfig(json.get("grid"));
            
            saveConfig = new SaveConfig(json.get("savegame"));
            backgroundBuilderConfig = new BackgroundBuilder.Config(json.get("backgroundBuilder"));
            borderBuilderConfig = new BorderBuilder.Config(json.get("borderBuilder"));
            dividerBuilderConfig = new DividerBuilder.Config(json.get("dividerBuilder"));
        }
        
        public MenuConfig getMenuConfig() {
            return menuConfig;
        }
        
        public HUDConfig getHUDConfig() {
            return hudConfig;
        }
        
        public GridConfig getGridConfig() {
            return gridConfig;
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
        
    }
    
    public static class MenuConfig extends EnumerizedJsonBasedProperties<MenuConfigValues> {

        public MenuConfig(JsonValue json) {
            super(json, MenuConfigValues.class);
        }
        
    }
    
    public static class HUDConfig extends EnumerizedJsonBasedProperties<HUDConfigValues> {
        
        public HUDConfig(JsonValue json) {
            super(json, HUDConfigValues.class);
        }
        
    }
    
    public static class GridConfig extends EnumerizedJsonBasedProperties<GridConfigValues> {
        
        public GridConfig(JsonValue json) {
            super(json, GridConfigValues.class);
        }
        
    }
        
}