package com.upseil.game.desktop;

import static com.upseil.game.Constants.GameInit.*;

import java.io.IOException;
import java.nio.file.Paths;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.upseil.game.Constants.GameInit;
import com.upseil.game.GameApplication;
import com.upseil.game.Savegame;
import com.upseil.game.SerializationContext;
import com.upseil.gdx.serialization.desktop.DesktopCompressingMapper;
import com.upseil.gdx.util.properties.Properties;

public class DesktopLauncher {
    
    private static boolean resizable;
    private static float widthHeightRatio = -1;
    private static int minWidth;
    private static int minHeight;
    private static int prefWidth;
    private static int prefHeight;
    
    private static int width;
    private static int height;
    
    public static void main(String[] args) {
        Properties<GameInit> gameInit;
        try {
            gameInit = Properties.fromPropertiesLines(java.nio.file.Files.readAllLines(Paths.get("game.init")), GameInit.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read the init file", e);
        }
        loadSizeInformation(gameInit);
        
        DisplayMode display = LwjglApplicationConfiguration.getDesktopDisplayMode();
        int windowPadding = gameInit.getInt(WindowPadding);
        calculateSize(display.width - windowPadding, display.height - windowPadding);
        
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = gameInit.get(Title);
        configuration.samples = gameInit.getInt(MsaaSamples);
        configuration.width = width;
        configuration.height = height;
        configuration.resizable = resizable;
        configuration.addIcon("icon/icon-128.png", Files.FileType.Internal);
        configuration.addIcon("icon/icon-32.png", Files.FileType.Internal);
        configuration.addIcon("icon/icon-16.png", Files.FileType.Internal);
        
        DesktopCompressingMapper<Savegame> savegameMapper = new DesktopCompressingMapper<>(Savegame.class);
        savegameMapper.setCompressing(true);
        
        SerializationContext context = new SerializationContext(savegameMapper);
        new LwjglApplication(new ResizeHook(new GameApplication(context)), configuration);
    }

    private static void loadSizeInformation(Properties<GameInit> gameInit) {
        resizable = gameInit.getBoolean(Resizable);
        
        if (gameInit.contains(Width) && gameInit.contains(Height)) {
            minWidth = gameInit.getInt(Width);
            minHeight = gameInit.getInt(Height);
            prefWidth = minWidth;
            prefHeight = minHeight;
            widthHeightRatio = (float) minWidth / minHeight;
            resizable = false;
        } else {
            minWidth = gameInit.getInt(MinWidth, 0);
            minHeight = gameInit.getInt(MinHeight, 0);
            if (minWidth > 0 && minHeight > 0) {
                widthHeightRatio = (float) minWidth / minHeight;
            }
            
            prefWidth = gameInit.getInt(PrefWidth, Integer.MAX_VALUE);
            prefHeight = gameInit.getInt(PrefHeight, Integer.MAX_VALUE);
            if (prefWidth == Integer.MAX_VALUE && prefHeight == Integer.MAX_VALUE) {
                if (minWidth > minHeight) {
                    prefHeight = toHeight(prefWidth);
                } else {
                    prefWidth = toWidth(prefHeight);
                }
            } else {
                widthHeightRatio = (float) prefWidth / prefHeight; 
            }
        }
    }

    private static void calculateSize(int windowWidth, int windowHeight) {
        if (widthHeightRatio <= 0) {
            width = windowWidth;
            height = windowHeight;
        } else {
            width = prefWidth;
            height = prefHeight;
            if (width > windowWidth) {
                width = Math.max(windowWidth, minWidth);
                height = toHeight(width);
            } 
            if (height > windowHeight) {
                height = Math.max(windowHeight, minHeight);
                width = toWidth(height);
            }
        }
    }
    
    private static int toHeight(int width) {
        if (widthHeightRatio <= 0) {
            return width;
        }
        return Math.round(width / widthHeightRatio);
    }
    
    private static int toWidth(int height) {
        if (widthHeightRatio <= 0) {
            return height;
        }
        return Math.round(height * widthHeightRatio);
    }
    
    private static class ResizeHook implements ApplicationListener {
        
        private final ApplicationListener listener;

        public ResizeHook(ApplicationListener listener) {
            this.listener = listener;
        }

        @Override
        public void resize(int newWidth, int newHeight) {
            int windowWidth = newWidth;
            int windowHeight = newHeight;
            
            boolean widthChanged = width != newWidth;
            boolean heightChanged = height != newHeight;
            if (widthChanged && heightChanged) {
                windowWidth = newWidth > newHeight ? newWidth : toWidth(newHeight);
                windowHeight = newHeight > newWidth ? newHeight : toHeight(newWidth);
            } else if (widthChanged) {
                windowHeight = toHeight(newWidth);
            } else if (heightChanged) {
                windowWidth = toWidth(newHeight);
            }
            
            DisplayMode display = Gdx.graphics.getDisplayMode();
            windowWidth = Math.min(windowWidth, display.width);
            windowHeight = Math.min(windowHeight, display.height);
            
            calculateSize(windowWidth, windowHeight);
            if (width != newWidth || height != newHeight) {
                Gdx.graphics.setWindowedMode(width, height);
            } else {
                listener.resize(newWidth, newHeight);
            }
        }

        @Override
        public void create() {
            listener.create();
        }

        @Override
        public void render() {
            listener.render();
        }

        @Override
        public void pause() {
            listener.pause();
        }

        @Override
        public void resume() {
            listener.resume();
        }

        @Override
        public void dispose() {
            listener.dispose();
        }
        
    }
    
}
