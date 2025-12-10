package view.renderers;

import model.entities.Bullet;
import view.asset.AssetKey;
import view.asset.AssetLoader;
import view.asset.ImageInterface;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.List;

/**
 * BulletRenderer - Renders all bullets in the game.
 */
public class BulletRenderer {
    private final AssetLoader assetLoader;

    public BulletRenderer(AssetLoader assetLoader) {
        this.assetLoader = assetLoader;
    }

    public void render(Graphics g, ImageObserver observer, List<Bullet> bullets) {
        for (Bullet bullet : bullets) {
            if (!bullet.isAlive()) {
                continue;
            }
            drawBullet(g, observer, bullet);
        }
    }

    private void drawBullet(Graphics g, ImageObserver observer, Bullet bullet) {
        ImageInterface bulletImage = assetLoader.getImage(AssetKey.BULLET_PEA.getId());
        if (bulletImage != null && bulletImage.isLoaded()) {
            g.drawImage(bulletImage.getImage(), (int) bullet.getX(), (int) bullet.getY(), observer);
        } else {
            // Fallback drawing if image is not loaded
            g.setColor(Color.YELLOW);
            g.fillOval((int) bullet.getX(), (int) bullet.getY(),(int) bullet.getWidth(),(int) bullet.getHeight());
        }
    }
}

