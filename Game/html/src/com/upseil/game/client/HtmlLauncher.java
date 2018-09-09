package com.upseil.game.client;

import static com.upseil.game.Constants.GameInit.*;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderCallback;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderState;
import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonSerializationContext;
import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.upseil.game.Constants.GameInit;
import com.upseil.game.GameApplication;
import com.upseil.game.Savegame;
import com.upseil.game.SerializationContext;
import com.upseil.game.domain.Color;
import com.upseil.gdx.gwt.serialization.HtmlCompressingMapper;
import com.upseil.gdx.gwt.util.BrowserConsoleLogger;
import com.upseil.gdx.util.format.DoubleFormatter;
import com.upseil.gdx.util.format.DoubleFormatter.Format;
import com.upseil.gdx.util.properties.Properties;

public class HtmlLauncher extends GwtApplication {
    
    public interface SavegameMapper extends ObjectMapper<Savegame> { }

    private static final String AutoHorizontalMargin = "auto-horizontal-margin";
    private static final String AutoVerticalMargin = "auto-vertical-margin";
    
    private static final Properties<GameInit> GameInit = Properties.fromPropertiesText(Resources.Instance.gameInitText().getText(), GameInit.class);
    
    private float widthHeightRatio = -1;
    private int minWidth;
    private int minHeight;
    private int prefWidth;
    private int prefHeight;
    
    private int width;
    private int height;
    
    @Override
    public void onModuleLoad() {
        super.setApplicationLogger(new BrowserConsoleLogger());
        setLogLevel(LOG_INFO);
        
        if (GameInit.contains(Width) && GameInit.contains(Height)) {
            minWidth = GameInit.getInt(Width);
            minHeight = GameInit.getInt(Height);
            prefWidth = minWidth;
            prefHeight = minHeight;
            widthHeightRatio = (float) minWidth / minHeight;
        } else {
            minWidth = GameInit.getInt(MinWidth, 0);
            minHeight = GameInit.getInt(MinHeight, 0);
            if (minWidth > 0 && minHeight > 0) {
                widthHeightRatio = (float) minWidth / minHeight;
            }
            
            prefWidth = GameInit.getInt(PrefWidth, Integer.MAX_VALUE);
            prefHeight = GameInit.getInt(PrefHeight, Integer.MAX_VALUE);
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
        super.onModuleLoad();
    }
    
    @Override
    public void setApplicationLogger(ApplicationLogger applicationLogger) {
        // No-op to prevent that the browser console logger is overwritten by super.onModuleLoad()
    }
    
    @Override
    public ApplicationListener createApplicationListener() {
        setLoadingListener(new LoadingListener() {
            @Override
            public void beforeSetup() {
            }
            @Override
            public void afterSetup() {
                setupResizing();
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
        Window.setMargin("0");
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                int clientWidth = event.getWidth();
                int clientHeight = event.getHeight();
                calculateSize(clientWidth, clientHeight);
                Window.enableScrolling(width > clientWidth || height > clientHeight);
                getGraphics().setWindowedMode(width, height);
                
                Panel rootPanel = getRootPanel();
                rootPanel.setSize(width + "px", height + "px");
                if (width > clientWidth) {
                    rootPanel.removeStyleName(AutoHorizontalMargin);
                } else {
                    rootPanel.addStyleName(AutoHorizontalMargin);
                }
                if (height > clientHeight) {
                    rootPanel.removeStyleName(AutoVerticalMargin);
                } else {
                    rootPanel.addStyleName(AutoVerticalMargin);
                }
            }
        });
    }
    
    @Override
    public GwtApplicationConfiguration getConfig() {
        int clientWidth = Window.getClientWidth();
        int clientHeight = Window.getClientHeight();
        calculateSize(clientWidth, clientHeight);
        
        Window.enableScrolling(width > clientWidth || height > clientHeight);
        Panel rootPanel = createGamePanel(width, height);
        if (width <= clientWidth) {
            rootPanel.addStyleName(AutoHorizontalMargin);
        }
        if (height <= clientHeight) {
            rootPanel.addStyleName(AutoVerticalMargin);
        }
        
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(width, height);
        configuration.preferFlash = false;
        configuration.rootPanel = rootPanel;
        configuration.antialiasing = GameInit.getInt(MsaaSamples) > 0;
        return configuration;
    }

    private void calculateSize(int clientWidth, int clientHeight) {
        if (widthHeightRatio <= 0) {
            width = clientWidth;
            height = clientHeight;
        } else {
            width = prefWidth;
            height = prefHeight;
            if (width > clientWidth) {
                width = Math.max(clientWidth, minWidth);
                height = toHeight(width);
            } 
            if (height > clientHeight) {
                height = Math.max(clientHeight, minHeight);
                width = toWidth(height);
            }
        }
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
        gamePanel.setWidth(width + "px");
        gamePanel.setHeight(height + "px");
        gamePanel.addStyleName("root");
        RootPanel.get().add(gamePanel);
        return gamePanel;
    }
    
    @Override
    public PreloaderCallback getPreloaderCallback() {
        FlowPanel dotContainer = new FlowPanel();
        dotContainer.addStyleName("loading-dot-container");

        Element[] dots = new Element[Color.size()];
        for (int i = 0; i < dots.length; i++) {
            String color = "var(--color" + i + ")";
            
            SimplePanel dot = new SimplePanel();
            dot.setSize("0%", "0%");
            dot.addStyleName("loading-dot");
            dot.getElement().getStyle().setBackgroundColor(color);
            
            SimplePanel dotWithBorder = new SimplePanel(dot);
            dotWithBorder.addStyleName("loading-dot-border");
            dotWithBorder.getElement().getStyle().setBorderColor(color);
            
            dotContainer.add(dotWithBorder);
            dots[i] = dot.getElement();
        }
        
        Label label = new Label("Loading");
        label.addStyleName("loading-label");
        
        VerticalPanel container = new VerticalPanel();
        container.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        container.addStyleName("loading-container");
        container.add(label);
        container.add(dotContainer);
        getRootPanel().add(container);
        
        float progressStep = 1.0f / dots.length;
        return new PreloaderCallback() {
            @Override
            public void error(String file) {
                HtmlLauncher.this.error("Preloading error", file);
            }
            @Override
            public void update(PreloaderState state) {
                float progress = state.getProgress();
                log("Preload Progress", progress + "");

                DoubleFormatter percentFormatter = DoubleFormatter.get(Format.Percent);
                for (int i = 0; i < dots.length; i++) {
                    Element dot = dots[i];
                    float dotProgress = Math.max(0, progress - i * progressStep) / progressStep;
                    dotProgress = Math.min(dotProgress, 1);

                    String size = percentFormatter.apply(dotProgress);
                    String margin = percentFormatter.apply(1 - dotProgress);
                    
                    Style style = dot.getStyle();
                    style.setProperty("width", size);
                    style.setProperty("height", size);
                    style.setProperty("margin", margin + " " + margin + " 0 0");
                }
            }           
        };
    }
    
    private int toHeight(int width) {
        if (widthHeightRatio <= 0) {
            return width;
        }
        return Math.round(width / widthHeightRatio);
    }
    
    private int toWidth(int height) {
        if (widthHeightRatio <= 0) {
            return height;
        }
        return Math.round(height * widthHeightRatio);
    }
    
}
