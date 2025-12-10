package model;


import model.entities.Plant;
import model.entities.Zombie;
import view.GamePanel;
import model.entities.Bullet;
import model.entities.Peashooter;
import model.entities.Sunflower;
import model.entities.PlantCard;
import view.asset.AssetKey;

import java.util.ArrayList;
import java.util.List;

public class GameLogicUpdater {
    private int sun;
    private final List<PlantCard> plantCards;
    private int zombieSpawnTimer = 0;
    private static final int ZOMBIE_SPAWN_INTERVAL = 500; // Spawn a zombie every 50 seconds at 10 FPS

    public GameLogicUpdater() {
        this.sun = 50; // Initial sun
        this.plantCards = new ArrayList<>();
        initializeCards();
    }

    private void initializeCards() {
        plantCards.add(new PlantCard("Peashooter", 100, AssetKey.CARD_PEASHOOTER));
        plantCards.add(new PlantCard("Sunflower", 50, AssetKey.CARD_SUNFLOWER));
        // plantCards.add(new PlantCard("CherryBomb", 150, AssetKey.CARD_CHERRYBOMB));
        // plantCards.add(new PlantCard("WallNut", 50, AssetKey.CARD_WALLNUT));
    }

    public int getSun() {
        return sun;
    }

    public List<PlantCard> getPlantCards() {
        return plantCards;
    }

    public void setSun(int sun) {
        this.sun = sun;
    }

    public void addSun(int amount) {
        this.sun += amount;
    }

    public void updateGameLogic(List<Plant> plants, List<Zombie> zombies, List<Bullet> bullets) {
        int sunflowerCount = 0;
        for (Plant p : plants) {
            p.update();
            if (p instanceof Sunflower) {
                sunflowerCount++;
            }
        }
        Sunflower.applyGlobalProductionBoost(sunflowerCount);

        // 2) 僵尸推进
        for (Zombie z : zombies) {
            z.update();
        }

        // 3) 触发豌豆射手攻击（生成子弹）
        for (Plant p : plants) {
            if (p instanceof Peashooter) {
                ((Peashooter) p).tryAttack(zombies, bullets);
            }
        }

        // 4) 子弹推进 + 清理
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(zombies, 1200); // Assuming viewport width
            if (!b.isAlive()) {
                bullets.remove(i);
            }
        }

        // 5) 统一清理死亡/越界实体
        plants.removeIf(p -> !p.isAlive());
        zombies.removeIf(z -> !z.isAlive() || (z.getX() + z.getWidth() < 0));

        // 6) 僵尸与植物碰撞检测
        for (Zombie z : zombies) {
            boolean isCurrentlyAttacking = false;
            for (Plant p : plants) {
                if (z.getRow() == p.getGridRow() && z.getBounds().intersects(p.getBounds())) {
                    isCurrentlyAttacking = true;
                    z.setAttacking(true);
                    if (z.canAttack()) {
                        z.attack(p);
                        z.resetAttackCooldown();
                    }
                    break; // A zombie can only attack one plant at a time
                }
            }
            if (!isCurrentlyAttacking) {
                z.setAttacking(false);
            }
        }

        // 7) Zombie Spawning
        zombieSpawnTimer++;
        if (zombieSpawnTimer >= ZOMBIE_SPAWN_INTERVAL) {
            zombieSpawnTimer = 0;
            int row = java.util.concurrent.ThreadLocalRandom.current().nextInt(5); // 0-4
            zombies.add(new Zombie(row));
            System.out.println("A new zombie has appeared in row " + row);
        }
    }
}