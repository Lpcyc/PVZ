package view;

import model.GameLogicUpdater;
import model.entities.*;
import view.GridConverter;
import view.asset.AssetLoader;
import view.renderers.GameRendererManager;
import model.entities.PlantCard;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GamePanel - 游戏视图主面板
 *
 * 职责：
 * - 固定视口并进行所有渲染入口（背景→实体→子弹→UI）；
 * - 暴露最小的可变状态访问器供 GameLogicUpdater/Controller 操作；
 * - 提供网格映射工具以保证控制器、渲染器与逻辑的一致坐标系。
 *
 * 线程/性能注意：
 * - repaint 与重绘节流应在此统一管理（requestRepaintThrottled）；
 * - updateGame 被 GameController 的 Timer 在 EDT 中调用，内部应避免长阻塞操作。
 */
public class GamePanel extends JPanel {
    public static final class Grid {
        public static final int ROWS = 5;
        public static final int COLS = 9;
        public static final int CELL_W = GridConverter.CELL_WIDTH;
        public static final int CELL_H = GridConverter.CELL_HEIGHT;
        public static final int ORIGIN_X = GridConverter.GRID_START_X;
        public static final int ORIGIN_Y = GridConverter.GRID_START_Y;

        public static int[] pointToGrid(int x, int y) {
            int col = (x - ORIGIN_X) / CELL_W;
            int row = (y - ORIGIN_Y) / CELL_H;
            if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return null;
            return new int[]{row, col};
        }

        public static int[] gridToPlantTopLeft(int row, int col, int plantW, int plantH) {
            int baseX = ORIGIN_X + col * CELL_W;
            int baseY = ORIGIN_Y + row * CELL_H;
            int x = baseX + (CELL_W - plantW) / 2;
            int y = baseY + (CELL_H - plantH);
            return new int[]{x, y};
        }

        public static int[] gridToCellOrigin(int row, int col) {
            int x = ORIGIN_X + col * CELL_W;
            int y = ORIGIN_Y + row * CELL_H;
            return new int[]{x, y};
        }
    }

    private final GameRendererManager rendererManager;
    private final GameLogicUpdater gameLogic;
    private final AssetLoader assetLoader;
    private final List<Plant> plants;
    private final List<Zombie> zombies;
    private final List<Bullet> bullets;
    private GameMouseListener mouseListener; // Mouse listener reference
    private PlantCard selectedPlantCard;
    private boolean plantingMode;

    private static final int CARD_DRAW_X = 120 + 85; // keep in sync with UIRenderer
    private static final int CARD_DRAW_Y = 5 + 5;
    private static final int CARD_WIDTH = 60;
    private static final int CARD_HEIGHT = 82;
    private static final int CARD_SPACING = 70;

    public GamePanel(GameLogicUpdater gameLogic) {
        this.gameLogic = gameLogic;
        this.assetLoader = AssetLoader.getInstance();
        this.rendererManager = new GameRendererManager(gameLogic, assetLoader);
        this.plants = new ArrayList<>();
        this.zombies = new ArrayList<>();
        this.bullets = new ArrayList<>();
        initializePanel();
        enablePanelMouseListener(true);
    }

    private void initializePanel() {
        setPreferredSize(new Dimension(1200, 600));
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        rendererManager.render(g2d, this, plants, zombies, bullets, selectedPlantCard, plantingMode);
    }

    public void updateGamePanel() {
        repaint();
    }

    public void updateGame() {
        gameLogic.updateGameLogic(plants, zombies, bullets);
        repaint();
    }

    public List<Plant> getPlants() {
        return plants;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public void setSunCount(int sun) { gameLogic.setSun(sun); }
    public int getSunCount() { return gameLogic.getSun(); }

    public boolean hasPlantAt(int row, int col) {
        for (Plant p : plants) if (p.getGridRow() == row && p.getGridCol() == col && p.isAlive()) return true;
        return false;
    }

    public boolean removePlantAt(int row, int col) {
        for (int i = 0; i < plants.size(); i++) {
            Plant p = plants.get(i);
            if (p.getGridRow() == row && p.getGridCol() == col) {
                plants.remove(i);
                return true;
            }
        }
        return false;
    }

    public void addPlant(Plant plant) {
        if (plant != null) plants.add(plant);
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null) zombies.add(zombie);
    }

    public void enablePanelMouseListener(boolean enabled) {
        if (enabled && mouseListener == null) {
            mouseListener = new GameMouseListener();
            addMouseListener(mouseListener);
        } else if (!enabled && mouseListener != null) {
            removeMouseListener(mouseListener);
            mouseListener = null;
        }
    }

    // Inner class for handling mouse events
    private class GameMouseListener extends java.awt.event.MouseAdapter {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            Point clickPoint = e.getPoint();
            if (tryCollectSun(clickPoint)) return;
            if (handleCardSelection(clickPoint)) return;
            if (plantingMode && selectedPlantCard != null) {
                handlePlantPlacement(clickPoint);
            }
        }
    }

    private boolean tryCollectSun(Point clickPoint) {
        for (Plant p : plants) {
            if (p instanceof Sunflower) {
                Sunflower sunflower = (Sunflower) p;
                if (sunflower.getBounds().contains(clickPoint) && sunflower.shouldProduceSun()) {
                    int collected = sunflower.collectSun();
                    if (collected > 0) {
                        gameLogic.addSun(collected);
                        System.out.println("Collected " + collected + " sun! Total: " + gameLogic.getSun());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean handleCardSelection(Point clickPoint) {
        int cardX = CARD_DRAW_X;
        int cardY = CARD_DRAW_Y;
        for (PlantCard card : gameLogic.getPlantCards()) {
            Rectangle cardBounds = new Rectangle(cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
            if (cardBounds.contains(clickPoint)) {
                if (gameLogic.getSun() >= card.getCost()) {
                    selectedPlantCard = card;
                    plantingMode = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    System.out.println("Selected " + card.getName());
                } else {
                    System.out.println("Not enough sun to select " + card.getName());
                }
                return true;
            }
            cardX += CARD_SPACING;
        }
        return false;
    }

    private void handlePlantPlacement(Point clickPoint) {
        int[] rc = Grid.pointToGrid(clickPoint.x, clickPoint.y);
        if (rc == null) {
            System.out.println("Click outside lawn. Planting canceled.");
            return;
        }
        int row = rc[0];
        int col = rc[1];
        if (hasPlantAt(row, col)) {
            System.out.println("Cell (" + row + "," + col + ") already occupied.");
            return;
        }
        if (gameLogic.getSun() < selectedPlantCard.getCost()) {
            System.out.println("Sun dropped below required cost. Planting canceled.");
            resetSelection();
            return;
        }
        Plant plant = createPlantFromCard(selectedPlantCard, row, col);
        if (plant == null) {
            System.out.println("Unknown plant type: " + selectedPlantCard.getName());
            resetSelection();
            return;
        }
        addPlant(plant);
        gameLogic.addSun(-selectedPlantCard.getCost());
        System.out.println("Planted " + selectedPlantCard.getName() + " at (" + row + "," + col + ")");
        resetSelection();
    }

    private void resetSelection() {
        plantingMode = false;
        selectedPlantCard = null;
        setCursor(Cursor.getDefaultCursor());
    }

    private Plant createPlantFromCard(PlantCard card, int row, int col) {
        String type = card.getPlantType();
        if ("Peashooter".equalsIgnoreCase(type)) {
            return new Peashooter(row, col);
        } else if ("PeashooterPlus".equalsIgnoreCase(type)) {
            return new PeashooterPlus(row, col);
        } else if ("Sunflower".equalsIgnoreCase(type)) {
            return new Sunflower(row, col);
        }
        return null;
    }
}
