package com.upseil.game.client;

import static com.upseil.game.Constants.GameInit.FixedSize;
import static com.upseil.game.Constants.GameInit.MinHeight;
import static com.upseil.game.Constants.GameInit.MinWidth;
import static com.upseil.game.Constants.GameInit.PrefHeight;
import static com.upseil.game.Constants.GameInit.PrefWidth;
import static com.upseil.game.Constants.GameInit.Title;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderCallback;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderState;
import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonSerializationContext;
import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.upseil.game.Constants.GameInit;
import com.upseil.game.GameApplication;
import com.upseil.game.Savegame;
import com.upseil.game.SerializationContext;
import com.upseil.gdx.gwt.serialization.HtmlCompressingMapper;
import com.upseil.gdx.gwt.util.BrowserConsoleLogger;
import com.upseil.gdx.util.properties.Properties;

public class HtmlLauncher extends GwtApplication {
    
    public interface SavegameMapper extends ObjectMapper<Savegame> { }
    
    private static final Properties<GameInit> GameInit = Properties.fromPropertiesText(Resources.Instance.gameInitText().getText(), GameInit.class);
    
    @Override
    public void onModuleLoad() {
        if (!GameInit.getBoolean(FixedSize)) {
            Window.enableScrolling(false);
        }
        super.onModuleLoad();
    }
    
    @Override
    public ApplicationListener createApplicationListener() {
        setLoadingListener(new LoadingListener() {
            @Override
            public void beforeSetup() {
                setApplicationLogger(new BrowserConsoleLogger());
            }
            @Override
            public void afterSetup() {
                // TODO Handle resizing for fixed size applications using min/pref/max width/height
                if (!GameInit.getBoolean(FixedSize)) {
                    setupResizing();
                }
            }
        });
        
        JsonSerializationContext serializationContext = JsonSerializationContext.builder().indent(false).build();
        JsonDeserializationContext deserializationContext = JsonDeserializationContext.builder().failOnUnknownProperties(false).build();
        
        SavegameMapper savegameMapper = GWT.create(SavegameMapper.class);
        HtmlCompressingMapper<Savegame> htmlSavegameMapper = new HtmlCompressingMapper<>(savegameMapper, serializationContext, deserializationContext);
        htmlSavegameMapper.setCompressing(true);
        
        SerializationContext context = new SerializationContext(htmlSavegameMapper);
        return new GameApplication(context);
    }
    
    private void setupResizing() {
        getRootPanel().setWidth("100%");
        getRootPanel().setHeight("100%");
        
        Window.setMargin("0");
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                getGraphics().setWindowedMode(event.getWidth(), event.getHeight());
            }
        });
    }
    
    @Override
    public GwtApplicationConfiguration getConfig() {
        int clientWidth = Window.getClientWidth();
        int clientHeight = Window.getClientHeight();
        
        int width, height;
        if (GameInit.getBoolean(FixedSize)) {
            width = GameInit.getInt(PrefWidth);
            height = GameInit.getInt(PrefHeight);
            if (width > clientWidth) {
                float ratio = (float) height / width;
                width = Math.max(clientWidth, GameInit.getInt(MinWidth));
                height = (int) (width * ratio);
            } 
            if (height > clientHeight) {
                float ratio = (float) width / height;
                height = Math.max(clientHeight, GameInit.getInt(MinHeight));
                width = (int) (height * ratio);
            }
        } else {
            width = clientWidth;
            height = clientHeight;
        }
        
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(width, height);
        configuration.preferFlash = false;
        configuration.rootPanel = createGamePanel(width, height);
        return configuration;
    }

    private Panel createGamePanel(int width, int height) {
        Panel gamePanel = new FlowPanel() {
            @Override
            public void onBrowserEvent(Event event) {
                int eventType = DOM.eventGetType(event);
                if (eventType == Event.ONMOUSEDOWN || eventType == Event.ONMOUSEUP) {
                    event.preventDefault();
                    event.stopPropagation();
                    Element target = event.getEventTarget().cast();
                    target.getStyle().setProperty("cursor", eventType == Event.ONMOUSEDOWN ? "default" : "");
                }
                super.onBrowserEvent(event);
            }
        };
        gamePanel.setWidth("" + width + "px");
        gamePanel.setHeight("" + height + "px");
        gamePanel.addStyleName("root");
        RootPanel.get().add(gamePanel);
        return gamePanel;
    }
    
    @Override
    public PreloaderCallback getPreloaderCallback() {
        final Label title = new Label(GameInit.get(Title));
        title.addStyleName("game-title");
        getRootPanel().add(title);
        
        return new PreloaderCallback() {
            @Override
            public void error (String file) {
                System.out.println("error: " + file);
            }
            @Override
            public void update (PreloaderState state) {
                // TODO Add some kind of loading state
            }           
        };
    }
    
}
