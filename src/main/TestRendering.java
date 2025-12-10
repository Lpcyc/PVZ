package main;

import model.GameLogicUpdater;
import view.GamePanel;
import view.asset.PreloadManager;

import javax.swing.*;

public class TestRendering {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1. Preload assets
            System.out.println("Starting asset preloading...");
            PreloadManager.preloadAssets(success -> {
                if (success) {
                    System.out.println("Asset preloading completed successfully.");
                    createAndShowGUI();
                } else {
                    System.err.println("Asset preloading failed. Exiting.");
                    // Optionally show a dialog to the user
                    JOptionPane.showMessageDialog(null, "Failed to load game assets. The application will now close.", "Loading Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Rendering Test - Plants vs. Zombies");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        GameLogicUpdater gameLogic = new GameLogicUpdater();
        GamePanel gamePanel = new GamePanel(gameLogic);
        gamePanel.enablePanelMouseListener(true);

        // 方便测试：预置足够阳光并摆放一组示例植物/僵尸
        gameLogic.setSun(250);
        setupTestEntities(gamePanel, gameLogic);

        frame.add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // GamePanel now handles its own input (card selection & planting).
        new Timer(100, e -> gamePanel.updateGame()).start();
    }

    private static void setupTestEntities(GamePanel gamePanel, GameLogicUpdater gameLogic) {
        gamePanel.getPlants().add(new model.entities.Peashooter(1, 1));
        gamePanel.getPlants().add(new model.entities.Sunflower(2, 3));
        gamePanel.getZombies().add(new model.entities.Zombie(2));
    }
}
