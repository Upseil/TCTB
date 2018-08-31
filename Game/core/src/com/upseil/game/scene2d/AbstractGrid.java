package com.upseil.game.scene2d;

import static com.upseil.game.Config.GridConfigValues.CellMoveSpeed;
import static com.upseil.game.Config.GridConfigValues.CellSize;
import static com.upseil.game.Config.GridConfigValues.Spacing;

import com.artemis.World;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.upseil.game.Config.GridConfig;
import com.upseil.game.domain.Color;
import com.upseil.gdx.math.ExtendedRandom;
import com.upseil.gdx.pool.PooledPools;

public class AbstractGrid extends Group {
    
    protected final Skin skin;
    protected final GridStyle style;
    protected final ExtendedRandom random;
    
    protected final Group cellGroup;
    protected final CellActor[][] cells;

    public AbstractGrid(World world, GridStyle style, ExtendedRandom random, int size) {
        this.skin = world.getRegistered("Skin");
        this.style = style;
        this.random = random;
        
        float worldSize = size * style.paddedCellSize;
        cellGroup = new Group();
        cellGroup.setBounds(0, 0, worldSize, worldSize);
        cells = new CellActor[size][size];
        initializeGrid();
        
        addActor(cellGroup);
    }
    
    protected void initializeGrid() {
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                if (cells[x][y] == null) {
                    createAndSetCell(x, y, Color.random(random.asRandom()));
                }
            }
        }
    }
    
    protected CellActor createAndSetCell(int x, int y, Color color) {
        CellActor cell = createCell(x, y, color);
        setCell(x, y, cell);
        return cell;
    }

    protected CellActor createCell(int x, int y, Color color) {
        CellActor cell = PooledPools.obtain(CellActor.class).initialize(skin, color, style.cellSize);
        cell.setPosition(toWorld(x), toWorld(y));
        cellGroup.addActor(cell);
        return cell;
    }
    
    protected CellActor getCell(int x, int y) {
        return cells[x][y];
    }

    protected void setCell(int x, int y, CellActor cell) {
        cells[x][y] = cell;
    }
    
    public int toGrid(float world) {
        return (int) (world / style.paddedCellSize);
    }
    
    public float toWorld(int grid) {
        return grid * style.paddedCellSize + style.cellOffset;
    }
    
    public int getGridWidth() {
        return cells.length;
    }
    
    public int getGridHeight() {
        return cells[0].length;
    }

    public float getWorldWidth() {
        return cellGroup.getWidth();
    }

    public float getWorldHeight() {
        return cellGroup.getHeight();
    }

    public static class GridStyle {
        
        public final float cellSize;
        public final float cellSpacing;
        public final float paddedCellSize;
        public final float cellOffset;
        public final float cellMoveSpeed;
        
        public GridStyle(GridConfig config) {
            this(config.getFloat(CellSize), config.getFloat(Spacing), config.getFloat(CellMoveSpeed));
        }
        
        public GridStyle(float cellSize, float spacing, float cellMoveSpeed) {
            this.cellSize = cellSize;
            this.cellSpacing = spacing;
            this.paddedCellSize = cellSize + spacing;
            this.cellOffset = spacing / 2;
            this.cellMoveSpeed = cellMoveSpeed;
        }
        
    }
    
}
