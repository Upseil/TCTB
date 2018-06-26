package com.upseil.game.system;

import com.upseil.game.Savegame;
import com.upseil.game.Tag;
import com.upseil.game.component.GameState;
import com.upseil.gdx.artemis.ArtemisConfigs.SaveConfig;
import com.upseil.gdx.artemis.system.AbstractLoadSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.serialization.Reader;

public class LoadSystem extends AbstractLoadSystem<Savegame> {
    
    private TagManager<Tag> tagManager;
    
    public LoadSystem(Reader<Savegame> mapper, SaveConfig config) {
        super(mapper, config);
    }

    @Override
    protected void loadGame(Savegame savegame) {
        GameState gameState = savegame.getGameState();
        world.edit(tagManager.getEntityId(Tag.GameState)).add(gameState);
    }
    
}
