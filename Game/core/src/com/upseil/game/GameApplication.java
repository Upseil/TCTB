package com.upseil.game;

import com.artemis.World;
import com.artemis.WorldConfiguration;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.I18NBundle;
import com.upseil.game.scene2d.HUDStage;
import com.upseil.game.system.GameInitializer;
import com.upseil.game.system.GridController;
import com.upseil.game.system.LoadSystem;
import com.upseil.game.system.SaveSystem;
import com.upseil.gdx.artemis.ArtemisApplicationAdapter;
import com.upseil.gdx.artemis.system.AllSubscriptionMisplacementWorkaround;
import com.upseil.gdx.artemis.system.ClearScreenSystem;
import com.upseil.gdx.artemis.system.EventSystem;
import com.upseil.gdx.artemis.system.LayeredInputSystem;
import com.upseil.gdx.artemis.system.LayeredSceneRenderSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.math.ExtendedRandomXS128;
import com.upseil.gdx.scene2d.util.BackgroundBuilder;
import com.upseil.gdx.scene2d.util.BorderBuilder;
import com.upseil.gdx.scene2d.util.DividerBuilder;

public class GameApplication extends ArtemisApplicationAdapter {

    public static final ExtendedRandom Random = new ExtendedRandomXS128();
    public static HUDStage HUD;
    
    private final SerializationContext serializationContext;
    
    private GameConfig config;
    private I18NBundle hudMessages;
    private Skin skin;
    
    private ShaderProgram shader;
    
    public GameApplication(SerializationContext serializationContext) {
        this.serializationContext = serializationContext;
    }
    
    @Override
    protected void setupWorldCreation() {
        config = new GameConfig("game-config.json");
        BackgroundBuilder.setConfig(config.getBackgroundBuilderConfig());
        BorderBuilder.setConfig(config.getBorderBuilderConfig());
        DividerBuilder.setConfig(config.getDividerBuilderConfig());
        
        I18NBundle.setSimpleFormatter(true);
        hudMessages = I18NBundle.createBundle(Gdx.files.internal("locale/UI"));
        
        skin = loadSkin("skin/tixel-vis/tixel-vis.json");

        FileHandle vertexShader = Gdx.files.internal("shader/default.vert");
        FileHandle fragmentShader = Gdx.files.internal("shader/grayscalable.frag");
        shader = new ShaderProgram(vertexShader, fragmentShader);
        if (shader.isCompiled() == false) {
            throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        }
    }

    @Override
    protected World createWorld() {
        WorldConfiguration worldConfiguration = new WorldConfigurationBuilder()
                .with(new AllSubscriptionMisplacementWorkaround())
                
                .with(new TagManager<Tag>())
                .with(new GameInitializer())
                
                .with(new LoadSystem(serializationContext.getSavegameMapper(), config.getSavegameConfig()))
                .with(new GridController())
                .with(new SaveSystem(serializationContext.getSavegameMapper(), config.getSavegameConfig()))
                
                .with(new LayeredInputSystem())
                .with(new ClearScreenSystem())
                .with(new LayeredSceneRenderSystem<>(new PolygonSpriteBatch(1000, shader)))

                .with(new EventSystem())
                
                .build();

        worldConfiguration.register("Config", config);
        worldConfiguration.register("UI", hudMessages);
        worldConfiguration.register("Skin", skin);
        
        return new World(worldConfiguration);
    }
    
//    @Override
//    protected void render(float deltaTime) {
//        super.render(Math.min(deltaTime, 0.5f));
//    }
    
    @Override
    public void dispose() {
        getWorld().dispose();
        skin.dispose();
        shader.dispose();
    }
    
}
