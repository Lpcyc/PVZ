package model.entities;

import java.awt.Rectangle;
import java.util.List;

/**
 * 强化版豌豆射手：射击更快、子弹更快且伤害更高。
 */
public class PeashooterPlus extends Plant implements Entity.GridAligned, Entity.AnimatedSprite {
    private static final int SHOOT_COOLDOWN_TICKS = 20;
    private static final int BULLET_W = 28, BULLET_H = 28;
    private static final int BULLET_DAMAGE = 20;
    private static final int BULLET_SPEED = 24;
    private int attackCooldown = 0;

    public PeashooterPlus(int row, int column) {
        super(row, column, 150, 120);
    }

    public PeashooterPlus(int x, int y, int row, int column) {
        super(x, y, row, column, 150, 120);
    }

    @Override
    public void update() {
        super.update();
        if (attackCooldown > 0) attackCooldown--;
    }

    public void tryAttack(List<Zombie> zombies, List<Bullet> bullets) {
        if (!alive || attackCooldown > 0) return;
        Rectangle pb = this.getBounds();
        boolean enemyAhead = false;
        for (Zombie z : zombies) {
            if (!z.isAlive()) continue;
            if (z.getRow() == this.row && z.getBounds().x > pb.x) {
                enemyAhead = true;
                break;
            }
        }
        if (!enemyAhead) return;
        int sx = (int) (pb.x + pb.width - 10);
        int sy = (int) (pb.y + (pb.height - BULLET_H) / 2);
        Bullet b = new Bullet(sx, sy, BULLET_DAMAGE, BULLET_SPEED);
        bullets.add(b);
        attackCooldown = SHOOT_COOLDOWN_TICKS;
    }

    @Override
    public String getSpriteKey() {
        return "plant_peashooter";
    }
}

