package model.entities;

import view.GridConverter;

/**
 * 僵尸类
 * 属性：
 * - 最大生命 200，初始移动速度 1.0f，攻击力 10；
 * - 所在行 row（0-4）。
 * 行为：
 * - 若存活则每帧向左平滑移动（像素坐标体系）；
 * - attack(Plant) 直接对目标造成伤害（由系统负责碰撞检测与选目标）。
 */
public class Zombie extends Entity implements Entity.AnimatedSprite {
    private float speed;   // 僵尸移动速度（像素/帧）
    private int damage;    // 攻击力
    private int row;       // 所在的行（0-4）
    private boolean isAttacking = false; // Is the zombie currently attacking?
    private int attackCooldown = 0; // Cooldown timer for attacks

    // 新增：默认速度常量（适当降低）
    private static final float DEFAULT_SPEED = 0.6f;
    private static final int ATTACK_INTERVAL_TICKS = 50; // Time between attacks

    public Zombie(int row) {
        this(GridConverter.colToX(8) + GridConverter.CELL_WIDTH, GridConverter.rowToY(row), row);
    }

    public Zombie(int x, int y, int row) {
        super(x, y, 40, 100, 200);
        this.speed = DEFAULT_SPEED; // 调整后默认速度
        this.damage = 10;
        this.row = row;
    }

    @Override
    public void update() {
        if (alive) {
            // Only move if not attacking
            if (!isAttacking) {
                move(-speed, 0);
            }
            // Update attack cooldown
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            // 越界自清理：完全离开屏幕左侧后标记死亡，便于 Updater 统一清除
            if (this.x + this.width < 0) {
                this.alive = false;
            }
        }
    }

    /** 对植物造成一次伤害（由系统控制调用频率与命中判定） */
    public void attack(Plant plant) {
        if (plant != null && plant.isAlive()) {
            plant.takeDamage(damage);
        }
    }

    // ===== AnimatedSprite 资源键 =====
    @Override
    public String getSpriteKey() {
        return "zombie_basic";
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public void setAttacking(boolean attacking) {
        isAttacking = attacking;
    }

    public boolean canAttack() {
        return attackCooldown <= 0;
    }

    public void resetAttackCooldown() {
        this.attackCooldown = ATTACK_INTERVAL_TICKS;
    }

    // ===== Getter/Setter =====
    public int getRow() {
        return row;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0f, speed);
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = Math.max(0, damage);
    }

    // 可按需覆写 onDeath() 做清理或特效
    // @Override
    // protected void onDeath() { /* ... */ }
}