package view.renderers;

import model.GameLogicUpdater;
import model.entities.Bullet;
import model.entities.Plant;
import model.entities.PlantCard;
import model.entities.Zombie;
import view.asset.AssetLoader;
import view.asset.AssetKey;
import view.asset.ImageInterface;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GameRendererManager {
    private static final int LAWN_MOWER_X_OFFSET = 180; // Position before first column
    private static final int LAWN_MOWER_SIZE = 50;
    private static final int GRID_ROWS = 5;
    private static final int GRID_COLS = 9;
    
    private final BackgroundRenderer backgroundRenderer;
    private final UIRenderer uiRenderer;
    private final EntityRenderers entityRenderers;
    private final BulletRenderer bulletRenderer;
    private final GameLogicUpdater gameLogic;
    private final AssetLoader assetLoader;

    public GameRendererManager(GameLogicUpdater gameLogic, AssetLoader assetLoader) {
        this.gameLogic = gameLogic;
        this.assetLoader = assetLoader;
        this.backgroundRenderer = new BackgroundRenderer(assetLoader);
        this.uiRenderer = new UIRenderer(assetLoader, gameLogic);
        this.entityRenderers = new EntityRenderers(assetLoader);
        this.bulletRenderer = new BulletRenderer(assetLoader);
    }

    public void render(Graphics2D g2d, JComponent observer, List<Plant> plants, List<Zombie> zombies, List<Bullet> bullets, PlantCard selectedCard, boolean plantingMode) {
        backgroundRenderer.render(g2d, observer);
        drawLawnMowers(g2d);
        if (plantingMode && selectedCard != null) {
            drawPlacementPreview(g2d, plants);
        }
        entityRenderers.render(g2d, observer, plants, zombies);
        bulletRenderer.render(g2d, observer, bullets);
        uiRenderer.render(g2d, selectedCard, plantingMode);
    }
    
    public void updateMousePosition(Point pos) {
        uiRenderer.setMousePosition(pos);
    }
    
    private void drawLawnMowers(Graphics2D g2d) {
        ImageInterface lawnMowerImage = assetLoader.getImage(AssetKey.UI_LAWNMOWER.getId());
        int gridStartY = view.GridConverter.GRID_START_Y;
        int cellHeight = view.GridConverter.CELL_HEIGHT;
        
        for (int row = 0; row < GRID_ROWS; row++) {
            int mowerY = gridStartY + row * cellHeight + (cellHeight - LAWN_MOWER_SIZE) / 2;
            
            if (lawnMowerImage != null && lawnMowerImage.isLoaded()) {
                g2d.drawImage(lawnMowerImage.getImage(), LAWN_MOWER_X_OFFSET, mowerY, LAWN_MOWER_SIZE, LAWN_MOWER_SIZE, null);
            } else {
                // Fallback if image not loaded
                g2d.setColor(new Color(150, 150, 150));
                g2d.fillRect(LAWN_MOWER_X_OFFSET, mowerY, LAWN_MOWER_SIZE, LAWN_MOWER_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(LAWN_MOWER_X_OFFSET, mowerY, LAWN_MOWER_SIZE, LAWN_MOWER_SIZE);
            }
        }
    }
    
    private void drawPlacementPreview(Graphics2D g2d, List<Plant> plants) {
        Point mousePos = uiRenderer.getMousePosition();
        if (mousePos == null || mousePos.x < 0) return;
        
        int gridStartX = view.GridConverter.GRID_START_X;
        int gridStartY = view.GridConverter.GRID_START_Y;
        int cellWidth = view.GridConverter.CELL_WIDTH;
        int cellHeight = view.GridConverter.CELL_HEIGHT;
        
        int col = (mousePos.x - gridStartX) / cellWidth;
        int row = (mousePos.y - gridStartY) / cellHeight;
        
        if (col >= 0 && col < GRID_COLS && row >= 0 && row < GRID_ROWS) {
            int cellX = gridStartX + col * cellWidth;
            int cellY = gridStartY + row * cellHeight;
            
            // Check if cell is occupied
            boolean occupied = false;
            for (Plant p : plants) {
                if (p.getGridRow() == row && p.getGridCol() == col && p.isAlive()) {
                    occupied = true;
                    break;
                }
            }
            
            if (occupied) {
                // Red highlight for occupied cell
                g2d.setColor(new Color(255, 100, 100, 80));
                g2d.fillRect(cellX, cellY, cellWidth, cellHeight);
                g2d.setColor(new Color(255, 50, 50, 150));
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawRect(cellX, cellY, cellWidth, cellHeight);
            } else {
                // Green highlight for valid placement
                g2d.setColor(new Color(100, 255, 100, 80));
                g2d.fillRect(cellX, cellY, cellWidth, cellHeight);
                g2d.setColor(new Color(50, 255, 50, 150));
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawRect(cellX, cellY, cellWidth, cellHeight);
            }
        }
    }
    
    public Point getMousePosition() {
        return uiRenderer.getMousePosition();
    }
}