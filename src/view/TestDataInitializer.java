package view;

import model.entities.Peashooter;
import model.entities.Plant;
import model.entities.Sunflower;
import model.entities.Zombie;

import java.util.List;

public class TestDataInitializer {
    private static final int LAWN_START_X = 250;
    private static final int LAWN_START_Y = 75;
    private static final int GRID_CELL_WIDTH = 83;
    private static final int GRID_CELL_HEIGHT = 100;

    public static void initializeTestData(List<Plant> plants, List<Zombie> zombies) {
        // 创建测试僵尸
        Zombie testZombie = new Zombie(700, LAWN_START_Y + 2 * GRID_CELL_HEIGHT, 2);
        zombies.add(testZombie);

        // 创建测试植物
        Sunflower sunflower = new Sunflower(
                LAWN_START_X + GRID_CELL_WIDTH,
                LAWN_START_Y + GRID_CELL_HEIGHT,
                1, 1
        );
        plants.add(sunflower);

        Peashooter peashooter = new Peashooter(
                LAWN_START_X + 2 * GRID_CELL_WIDTH,
                LAWN_START_Y + 3 * GRID_CELL_HEIGHT,
                3, 2
        );
        plants.add(peashooter);

        System.out.println("测试数据初始化完成");
    }
}