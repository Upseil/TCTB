package com.upseil.game.component;

import com.artemis.Component;
import com.upseil.game.domain.Grid;

public class GridComponent extends Component {
    
    private Grid grid;

    public Grid getGrid() {
        return grid;
    }

    public GridComponent setGrid(Grid grid) {
        this.grid = grid;
        return this;
    }
    
}
