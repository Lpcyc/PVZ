package model.entities;

import java.awt.Rectangle;

/**
 * 通用实体基类（演进版）
 *
 * 设计要点：
 * - 统一像素坐标（x/y/width/height），不强制网格；
 * - 简化生命系统：current/max（int），alive 标志；
 * - 统一生命周期：update()/takeDamage()/kill()/getBounds() 等；
 * - 不感知渲染与资源，仅 Model 抽象。
 *
 * 统一接口（供子类按需实现）：
 * - GridAligned：有明确的网格语义（如植物），需提供 getGridRow/getGridCol/setGridPosition；
 * - AnimatedSprite：能提供渲染资源键（如 "plant_peashooter"/"zombie_basic"）。
 *
 * 重载约定：
 * - 缺省仅需覆写 update()；
 * - 如需依赖上下文（例如子弹需要僵尸列表与视口宽度），可覆写 update(List<Zombie>, int viewportWidth)，
 *   基类提供了默认实现，会回落到 update()，确保多态调用安全。
 */
public abstract class Entity {
    // 像素坐标与尺寸
    protected float x;
    protected float y;
    protected float width;
    protected float height;

    // 简化后的生命系统（整型，契合当前 Plant/Zombie 设定）
    protected int currentHealth;
    protected int maxHealth;
    protected boolean alive = true;

    protected Entity(float x, float y, float width, float height, int maxHealth) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxHealth = Math.max(1, maxHealth);
        this.currentHealth = this.maxHealth;
    }

    // 位置与移动
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void move(float dx, float dy) { this.x += dx; this.y += dy; }

    // 生命与伤害（返回本次实际伤害）
    public int takeDamage(int rawDamage) {
        if (!alive || rawDamage <= 0) return 0;
        int applied = Math.min(currentHealth, rawDamage);
        currentHealth -= applied;
        if (currentHealth <= 0) {
            currentHealth = 0;
            alive = false;
            onDeath();
        }
        return applied;
    }
    @Deprecated
    public int takeDamage(double rawDamage) {
        return takeDamage((int) Math.max(0, Math.round(rawDamage)));
    }

    public void heal(int amount) {
        if (!alive || amount <= 0) return;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    // 主生命周期
    public abstract void update();

    /**
     * 上下文更新（默认回落到无参 update）
     * 用途：少量实体（如 Bullet）需要僵尸列表与视口宽度做碰撞/越界判断。
     * 子类如不需要上下文，保持默认实现即可。
     */
    public void update(java.util.List<Zombie> zombies, int viewportWidth) {
        update();
    }

    protected void onDeath() { /* 子类可覆写清理/掉落/特效 */ }
    public void kill() {
        if (alive) {
            currentHealth = 0;
            alive = false;
            onDeath();
        }
    }

    // 查询
    public boolean isAlive() { return alive; }
    public int getCurrentHealth() { return currentHealth; }
    public int getMaxHealth() { return maxHealth; }
    public float getHealthPercent() {
        return maxHealth == 0 ? 0f : (float) currentHealth / (float) maxHealth;
    }

    public Rectangle getBounds() {
        return new Rectangle(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
    }

    // 基础坐标/尺寸访问器
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    // ==================== 统一接口（供具体子类按需实现） ====================
    /** 需要网格语义的实体（如植物） */
    public interface GridAligned {
        int getGridRow();
        int getGridCol();
        void setGridPosition(int row, int col);
    }

    /** 提供资源键名以供渲染层加载贴图 */
    public interface AnimatedSprite {
        String getSpriteKey();
    }
}
