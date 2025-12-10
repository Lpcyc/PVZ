package model.entities;

import view.asset.AssetLoader;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

public class Bullet extends Entity {
    private int damage;//子弹伤害
    private int speed;//子弹速度

    public Bullet(int x, int y, int damage, int speed) {
        super(x, y, 28, 28, 1); // 统一子弹尺寸 28x28
        this.damage = damage;
        this.speed = speed;
    }

    @Override
    public void update() {
        // 简单前进（无碰撞版本）
        this.x += speed;
    }

    // 覆盖：带上下文的更新（含碰撞与越界）
    @Override
    public void update(java.util.List<Zombie> zombies, int viewportWidth) {
        if (!alive) return;
        this.x += speed;

        // 越界则标记死亡
        if (this.x > viewportWidth) {
            this.alive = false;
            return;
        }

        // 碰撞检测：与任意僵尸 AABB 相交则命中
        Rectangle pr = this.getBounds();
        for (Zombie z : zombies) {
            if (!z.isAlive()) continue;
            if (pr.intersects(z.getBounds())) {
                z.takeDamage(damage);
                this.alive = false;
                break;
            }
        }
    }

    // 渲染（资源在此类内按需加载，减轻上层负担）
    public void render(Graphics2D g2, AssetLoader loader, Component observer) {
        if (!alive) return;
        Image img = loader.getAwtImage("bullet_pea");
        if (img == null) {
            // 首次加载（显式路径兜底）
            loader.loadImage("bullet_pea", "/bullet/PeaNormal.png");
            img = loader.getAwtImage("bullet_pea");
        }
        if (img != null) {
            g2.drawImage(img, Math.round(x), Math.round(y), Math.round(width), Math.round(height), observer);
        } else {
            // 回退绘制
            g2.setColor(new java.awt.Color(80, 200, 80));
            g2.fillOval(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
        }
    }

    // Getter方法
    public int getDamage() { return damage; }
    public int getSpeed() { return speed; }

}
