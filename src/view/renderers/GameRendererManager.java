package view.renderers;

import model.GameLogicUpdater;
import model.entities.Bullet;
import model.entities.Plant;
import model.entities.PlantCard;
import model.entities.Zombie;
import view.asset.AssetLoader;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GameRendererManager {
    private final BackgroundRenderer backgroundRenderer;
    private final UIRenderer uiRenderer;
    private final EntityRenderers entityRenderers;
    private final BulletRenderer bulletRenderer;
    private final GameLogicUpdater gameLogic;

    public GameRendererManager(GameLogicUpdater gameLogic, AssetLoader assetLoader) {
        this.gameLogic = gameLogic;
        this.backgroundRenderer = new BackgroundRenderer(assetLoader);
        this.uiRenderer = new UIRenderer(assetLoader, gameLogic);
        this.entityRenderers = new EntityRenderers(assetLoader);
        this.bulletRenderer = new BulletRenderer(assetLoader);
    }

    public void render(Graphics2D g2d, JComponent observer, List<Plant> plants, List<Zombie> zombies, List<Bullet> bullets, PlantCard selectedCard, boolean plantingMode) {
        backgroundRenderer.render(g2d, observer);
        entityRenderers.render(g2d, observer, plants, zombies);
        bulletRenderer.render(g2d, observer, bullets);
        uiRenderer.render(g2d, selectedCard, plantingMode);
    }
}