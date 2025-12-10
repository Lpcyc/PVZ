package model.entities;

import java.awt.Rectangle;
import java.util.List;

/**
 * 豌豆射手
 * 属性：
 * - 生命值 100，成本 100；
 * - 每 100 帧触发一次“发射”事件（由控制器 Tick 驱动 update）。
 * 说明：
 * - row/column 由 Plant 基类维护；此类仅使用其只读显示。
 */
public class Peashooter extends Plant implements Entity.GridAligned, Entity.AnimatedSprite {
    // Combat parameters and cooldown
    private static final int SHOOT_COOLDOWN_TICKS = 30; // Fires roughly every 0.6s @50FPS
    private static final int BULLET_W = 28, BULLET_H = 28;
    private static final int BULLET_DAMAGE = 10;
    private static final int BULLET_SPEED = 18;
    private int attackCooldown = 0;

    public Peashooter(int row, int column) {
        // (row, column, cost, health)
        super(row, column, 100, 100); // Cost 100 sun, health 100
        this.attackCooldown = 0;
    }

    public Peashooter(int x, int y, int row, int column) {
        super(x, y, row, column, 100, 100);
        this.attackCooldown = 0;
    }

    @Override
    public void update() {
        super.update();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

    /** Called by the logic updater: attempts to fire a bullet if conditions are met. */
    public void tryAttack(List<Zombie> zombies, List<Bullet> bullets) {
        if (!alive || attackCooldown > 0) return;

        Rectangle pb = this.getBounds();
        boolean enemyAhead = false;
        for (Zombie z : zombies) {
            if (!z.isAlive()) continue;
            if (z.getRow() == this.row) {
                Rectangle zb = z.getBounds();
                if (zb.x > pb.x) { // Check if the zombie is in front of the peashooter
                    enemyAhead = true;
                    break;
                }
            }
        }
        if (!enemyAhead) return;

        // Fire a bullet (spawned from the plant's right-center)
        int sx = (int) (pb.x + pb.width - 10);
        int sy = (int) (pb.y + (pb.height - BULLET_H) / 2);
        Bullet b = new Bullet(sx, sy, BULLET_DAMAGE, BULLET_SPEED);
        bullets.add(b);

        // Reset cooldown
        attackCooldown = SHOOT_COOLDOWN_TICKS;
    }

    // ===== Implement Entity.AnimatedSprite =====
    @Override
    public String getSpriteKey() {
        return "plant_peashooter";
    }
}
