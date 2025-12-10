package view.renderers;

/**
 * EntityRenderers - 实体统一渲染入口

 * 设计要点：
 * - 统一绘制植物与僵尸，允许按类注册专用渲染器（TypedRenderer）以扩展行为；
 * - 优先使用 ImageInterface（若可用且已加载）并通过 ImageIcon.paintIcon 绘制以保证 GIF/序列帧的稳定显示；
 * - 对于未就绪的资源，触发 assetLoader.loadImage 做异步预热并回退到占位绘制或重绘请求。

 * 约定：
 * - render 在 EDT 中调用，observer 通常为调用的 Component；
 * - paintAtBounds/paintWithImageInterface 返回 false 表示绘制未完成（需要重试/等待资源）。
 */

import model.entities.Entity;
import model.entities.Plant;
import model.entities.Zombie;
import view.asset.AssetLoader;
import view.asset.ImageInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityRenderers {
    private final AssetLoader assetLoader;
    private final Map<Class<?>, TypedRenderer<? extends Entity>> registry = new HashMap<>();
    private final java.util.WeakHashMap<Image, ImageIcon> iconCache = new java.util.WeakHashMap<>();

    public interface TypedRenderer<T extends Entity> {
        void render(Graphics g, T entity);
        default int priority() { return 0; }
    }

    public EntityRenderers(AssetLoader assetLoader) {
        this.assetLoader = assetLoader;
    }

    public <T extends Entity> void registerRenderer(Class<T> type, TypedRenderer<T> renderer) { registry.put(type, renderer); }
    public void unregisterRenderer(Class<?> type) { registry.remove(type); }

    public void render(Graphics g, ImageObserver observer, List<Plant> plants, List<Zombie> zombies) {
        // 植物
        for (Plant plant : plants) {
            if (!plant.isAlive()) continue;
            TypedRenderer<Plant> pr = lookupRenderer(plant);
            if (pr != null) pr.render(g, plant);
            else drawPlant(g, observer, plant);
        }
        // 僵尸
        for (Zombie zombie : zombies) {
            if (!zombie.isAlive()) continue;
            TypedRenderer<Zombie> zr = lookupRenderer(zombie);
            if (zr != null) zr.render(g, zombie);
            else drawZombie(g, observer, zombie);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> TypedRenderer<T> lookupRenderer(T entity) {
        return (TypedRenderer<T>) registry.get(entity.getClass());
    }

    private ImageIcon iconFor(Image img) {
        if (img == null) return null;
        ImageIcon cached = iconCache.get(img);
        if (cached != null) return cached;
        ImageIcon icon = new ImageIcon(img);
        iconCache.put(img, icon);
        return icon;
    }

    /**
     * paintAtBounds - 在给定包围盒内按原始像素底对齐绘制 Image
     * 返回 true 表示成功绘制；false 表示资源尺寸不可用或需要等待。
     */
    private boolean paintAtBounds(Graphics g, ImageObserver observer, Rectangle b, Image img) {
        if (img == null) return false;
        int x = b.x, y = b.y, width = b.width, height = b.height;

        // 回退：不再在绘制前调用背景打底，直接绘制 Icon 或 Image
        if (observer instanceof Component) {
            ImageIcon icon = iconFor(img);
            int iw = icon.getIconWidth(), ih = icon.getIconHeight();
            if (iw <= 0 || ih <= 0) return false;
            int drawX = x + (width - iw) / 2;
            int drawY = y + (height - ih);
            icon.paintIcon((Component) observer, g, drawX, drawY);
            return true;
        } else {
            int iw = img.getWidth(observer), ih = img.getHeight(observer);
            if (iw <= 0 || ih <= 0) return false;
            int drawX = x + (width - iw) / 2;
            int drawY = y + (height - ih);
            g.drawImage(img, drawX, drawY, iw, ih, observer);
            return true;
        }
    }

    /**
     * paintWithImageInterface - 使用 ImageInterface 渲染（优先）
     * - 对于动画资源：若未播放则尝试自动 play() 以恢复播放（AssetLoader 可全局控制 pause/resume）
     */
    private boolean paintWithImageInterface(Graphics g, ImageObserver observer, Rectangle b, ImageInterface ii) {
        if (ii == null || !ii.isLoaded()) return false;
        // 动图若非播放中则自动恢复（全局暂停仍可通过 AssetLoader 控制）
        if (ii.isAnimated() && !ii.isPlaying()) {
            try { ii.play(); } catch (Exception ignore) {}
        }
        ImageIcon icon = ii.getImageIcon();
        int x = b.x, y = b.y, width = b.width, height = b.height;
        if (icon != null) {
            int iw = icon.getIconWidth(), ih = icon.getIconHeight();
            if (iw <= 0 || ih <= 0) return false;
            int drawX = x + (width - iw) / 2;
            int drawY = y + (height - ih);

            // 回退：不再进行背景打底
            if (observer instanceof Component) {
                icon.paintIcon((Component) observer, g, drawX, drawY);
            } else {
                g.drawImage(icon.getImage(), drawX, drawY, iw, ih, observer);
            }
            return true;
        }
        // 无 Icon 则回退到底层 Image
        return paintAtBounds(g, observer, b, ii.getImage());
    }

    /**
     * tryPaintByKeys - 按候选键链尝试渲染（从最具体到最通用）
     * 说明：若资源未加载则会触发异步 loadImage，函数返回 false 表示未能即时绘制。
     */
    private boolean tryPaintByKeys(Graphics g, ImageObserver observer, Rectangle b, String... keys) {
        for (String key : keys) {
            if (key == null || key.isEmpty()) continue;
            ImageInterface ii = assetLoader.getImage(key);
            if (ii != null && ii.isLoaded()) {
                if (paintWithImageInterface(g, observer, b, ii)) return true;
            } else {
                try { assetLoader.loadImage(key); } catch (Exception ignore) {}
            }
        }
        return false;
    }

    private void drawPlant(Graphics g, ImageObserver observer, Plant plant) {
        Rectangle b = ((Entity) plant).getBounds();
        String key1 = "plant_" + plant.getClass().getSimpleName().toLowerCase();
        String key2 = plant.getClass().getSimpleName().toLowerCase();
        String key3 = "plant";
        boolean ok = tryPaintByKeys(g, observer, b, key1, key2, key3);
        if (!ok) {
            Image img = assetLoader.getAwImage(key1);
            if (!paintAtBounds(g, observer, b, img)) {
                if (observer instanceof Component) ((Component) observer).repaint(b.x, b.y, b.width, b.height);
                drawPlantFallback(g, plant, b.x, b.y, b.width, b.height);
            }
        }
        drawHealthBar(g, plant, b.x, b.y, b.width, b.height);
    }

    private void drawZombie(Graphics g, ImageObserver observer, Zombie zombie) {
        Rectangle b = ((Entity) zombie).getBounds();
        String zombieType = zombie.getClass().getSimpleName();
        String key1;
        if ("BucketheadZombie".equalsIgnoreCase(zombieType)) {
            key1 = "zombie_buckethead";
        } else {
            key1 = "zombie_" + zombieType.toLowerCase();
        }
        String key2 = "zombie_basic";
        String key3 = "zombie";
        boolean ok = tryPaintByKeys(g, observer, b, key1, key2, key3);
        if (!ok) {
            Image img = assetLoader.getAwImage(key1);
            if (img == null) img = assetLoader.getAwImage(key2);
            if (!paintAtBounds(g, observer, b, img)) {
                if (observer instanceof Component) ((Component) observer).repaint(b.x, b.y, b.width, b.height);
                drawZombieFallback(g, zombie, b.x, b.y, b.width, b.height);
            }
        }
        drawHealthBar(g, zombie, b.x, b.y, b.width, b.height);
    }

    private void drawPlantFallback(Graphics g, Plant plant, int x, int y, int width, int height) {
        g.setColor(getPlantColor(plant));
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(plant.getClass().getSimpleName(), x + 5, y + height / 2);
    }

    private void drawZombieFallback(Graphics g, Zombie zombie, int x, int y, int width, int height) {
        g.setColor(Color.GRAY);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("ZOMBIE", x + 5, y + height / 2);
    }

    private void drawHealthBar(Graphics g, Object entity, int x, int y, int width, int height) {
        int health = getHealth(entity);
        int maxHealth = getMaxHealth(entity);
        if (maxHealth > 0 && health < maxHealth) {
            int barWidth = width, barHeight = 6;
            int barX = x, barY = y - 10;
            g.setColor(Color.RED);
            g.fillRect(barX, barY, barWidth, barHeight);
            g.setColor(Color.GREEN);
            int currentWidth = (int) (barWidth * ((double) health / maxHealth));
            g.fillRect(barX, barY, currentWidth, barHeight);
            g.setColor(Color.BLACK);
            g.drawRect(barX, barY, barWidth, barHeight);
        }
    }

    private int getHealth(Object entity) {
        return (entity instanceof Entity) ? (int) Math.round(((Entity) entity).getCurrentHealth()) : 0;
    }

    private int getMaxHealth(Object entity) {
        return (entity instanceof Entity) ? ((Entity) entity).getMaxHealth() : 0;
    }

    private Color getPlantColor(Plant plant) {
        switch (plant.getClass().getSimpleName()) {
            case "Sunflower": return Color.YELLOW;
            case "Peashooter": return Color.GREEN;
            default: return Color.WHITE;
        }
    }
}
