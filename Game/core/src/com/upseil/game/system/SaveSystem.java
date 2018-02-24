package com.upseil.game.system;

import com.artemis.ComponentMapper;
import com.upseil.game.Savegame;
import com.upseil.game.Tag;
import com.upseil.game.component.GameState;
import com.upseil.gdx.artemis.config.SaveConfig;
import com.upseil.gdx.artemis.system.AbstractSaveSystem;
import com.upseil.gdx.artemis.system.TagManager;
import com.upseil.gdx.serialization.Writer;

public class SaveSystem extends AbstractSaveSystem<Savegame> {
    
    private TagManager<Tag> tagManager;
    private ComponentMapper<GameState> gameStateMapper;
    
    public SaveSystem(Writer<Savegame> mapper, SaveConfig config) {
        super(mapper, config);
    }

    @Override
    protected Savegame getSavegame() {
        GameState gameState = gameStateMapper.get(tagManager.getEntityId(Tag.GameState));
        Savegame savegame = new Savegame(gameState);
        return savegame;
    }
    
}
