package com.upseil.game.component;

import com.artemis.Component;
import com.upseil.game.domain.Grid;

public class GridComponent extends Component {
    
    private Grid grid;

    public Grid get() {
        return grid;
    }

    public GridComponent set(Grid grid) {
        this.grid = grid;
        return this;
    }
    
}
