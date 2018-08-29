package com.upseil.game.desktop;

import static com.upseil.game.Constants.GameInit.*;

import java.io.IOException;
import java.nio.file.Paths;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.upseil.game.Constants.GameInit;
import com.upseil.game.GameApplication;
import com.upseil.game.Savegame;
import com.upseil.game.SerializationContext;
import com.upseil.gdx.serialization.desktop.DesktopCompressingMapper;
import com.upseil.gdx.util.properties.Properties;

public class DesktopLauncher {
    
    public static void main(String[] args) {
        Properties<GameInit> gameInit;
        try {
            gameInit = Properties.fromPropertiesLines(java.nio.file.Files.readAllLines(Paths.get("game.init")), GameInit.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read the init file", e);
        }
        
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = gameInit.get(Title);
        configuration.width = gameInit.getInt(Width);
        configuration.height = gameInit.getInt(Height);
        configuration.resizable = !gameInit.getBoolean(FixedSize);
        configuration.addIcon("icon/icon-128.png", Files.FileType.Internal);
        configuration.addIcon("icon/icon-32.png", Files.FileType.Internal);
        configuration.addIcon("icon/icon-16.png", Files.FileType.Internal);
        
        DesktopCompressingMapper<Savegame> savegameMapper = new DesktopCompressingMapper<>(Savegame.class);
        savegameMapper.setCompressing(true);
        
        SerializationContext context = new SerializationContext(savegameMapper);
        new LwjglApplication(new GameApplication(context), configuration);
    }
    
}
