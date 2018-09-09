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
        // Logo Animation
        LogoAnimation, FlyInDelay, FlyInDuration, BackgroundAlpha, BackgroundFadeOutDuration,
        AdditionalOutlinesFadeInDelay, OutlinesFadeInDuration, FillingsFadeInDuration,
        MaxAdditionalFillingsFadeInDelay, BackgroundBlurFadeInDuration, ShadowFadeInDuration,
        // Menu Style
        LogoTopPadding, LogoHorizontalPadding, LogoMoveDuration, ControlsFadeInDelay, ControlsFadeInDuration,
        ControlsTopPadding,
        // Background Grid
        GridSize, CellSize, CellSpacing, CellMoveSpeed,
            // Entrance Animation
            MaxAdditionalStripeInDelay, MaxAdditionalFadeInDelay, FadeInDuration, MaxAdditionalScaleInDelay, ScaleInDuration,
            // Action Animation
            GridActionInterval, ShiftLineChance, MaxConcurrentActions, MaxAdditionalActionDelay, GridActionDuration,
            ShakesPerAction, ShakeRotation, RumblesPerAction, RumbleDistance, SpinsPerAction,
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
            super(json, true, MenuConfigValues.class);
        }
        
    }
    
    public static class HUDConfig extends EnumerizedJsonBasedProperties<HUDConfigValues> {
        
        public HUDConfig(JsonValue json) {
            super(json, true, HUDConfigValues.class);
        }
        
    }
    
    public static class GridConfig extends EnumerizedJsonBasedProperties<GridConfigValues> {
        
        public GridConfig(JsonValue json) {
            super(json, true, GridConfigValues.class);
        }
        
    }
        
}