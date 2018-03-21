package com.upseil.game.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.upseil.game.GameApplication;
import com.upseil.game.Savegame;
import com.upseil.game.SerializationContext;
import com.upseil.gdx.serialization.desktop.DesktopCompressingMapper;

public class DesktopLauncher {
    
    public static void main(String[] args) {
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = "Stay Colorful";
        configuration.addIcon("icon/icon-128.png", Files.FileType.Internal);
        configuration.addIcon("icon/icon-32.png", Files.FileType.Internal);
        configuration.addIcon("icon/icon-16.png", Files.FileType.Internal);
        configuration.width = 1000;
        configuration.height = 1000;
        
        DesktopCompressingMapper<Savegame> savegameMapper = new DesktopCompressingMapper<>(Savegame.class);
        savegameMapper.setCompressing(true);
        
        SerializationContext context = new SerializationContext(savegameMapper);
        new LwjglApplication(new GameApplication(context), configuration);
    }
    
}
