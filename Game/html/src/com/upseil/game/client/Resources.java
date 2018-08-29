package com.upseil.game.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundle {
    
    Resources Instance = GWT.create(Resources.class);
    
    @Source("../game.init")
    TextResource gameInitText();
    
}
