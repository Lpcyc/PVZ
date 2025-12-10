package controller;

import model.entities.Plant;
import model.entities.Sunflower;
import model.entities.Zombie;
import view.GamePanel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * GameController - 游戏控制器
 *
 * 主要职责：
 * - 启动/暂停/恢复/停止游戏主循环（由 Swing Timer 驱动）；
 * - 管理僵尸刷怪（定时器）与行分布策略（洗牌袋 + 拥挤度均衡）；
 * - 处理玩家鼠标交互（放置/铲除），并委托 GamePanel 变更实体集合。
 *
 * 重要约定：
 * - 所有对 GamePanel 的调用均发生在 EDT（由 Timer 或鼠标事件触发）；
 * - spawnZombieRandom/similar 方法为外部可调用接口（可被按钮直接触发）。
 *
 * 扩展建议：
 * - 将行选择策略抽象为策略对象，便于替换/测试；
 * - 将 spawn 参数（速度、位置偏移）移到配置项以便 tuning。
 */
public class GameController {
    //配置常量
    private static final int GAME_TICK_MS = 50; // 每帧间隔（毫秒）
    private static final int MIN_ZOMBIE_SPAWN_MS = 3000; // 最小刷怪间隔
    private static final int MAX_ZOMBIE_SPAWN_MS = 6000; // 最大刷怪间隔

    //核心组件
    private GamePanel gamePanel;// 游戏面板（视图层）
    private Timer gameTimer;// 游戏循环计时器
    private boolean isRunning;
    private Timer zombieSpawnTimer;// 僵尸刷怪计时器

    //随机数生成
    private final java.util.Random rng = new java.util.Random();

    //交互系统/植物类型
    public enum InteractionMode { PLACE, SHOVEL }
    public enum PlantType { PEASHOOTER, SUNFLOWER }
    private InteractionMode interactionMode = InteractionMode.PLACE;
    private PlantType selectedPlantType = PlantType.PEASHOOTER;

    //刷怪系统
    // 新增：避免连续同一行（保留旧字段以兼容日志，但不再直接使用它选择行）
    private int lastSpawnRow = -1;
    // === 新：洗牌袋（保证每轮覆盖所有行，避免连续同一行）===
    private final java.util.ArrayDeque<Integer> rowBag = new java.util.ArrayDeque<>();



    public GameController(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.isRunning = false;
        initializeGameLoop();
        registerPlacementHandler();
        initializeZombieSpawner();
        // 初始化洗牌袋
        refillRowBag();
    }

    /** 初始化游戏循环（50ms/帧） */
    private void initializeGameLoop() {
        // 创建游戏循环计时器，每50毫秒更新一次（约20FPS）
        gameTimer = new Timer(GAME_TICK_MS, e -> {
            if (isRunning) {
                gamePanel.updateGame();
            }
        });
    }

    /** 注册鼠标点击：根据交互模式执行放置或铲除
     *  - 仅在运行中处理点击
     *  - 使用 GamePanel.Grid 将像素映射到网格（严格命中）
     */
    private void registerPlacementHandler() {
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 仅在运行中允许交互
                if (!isRunning) return;
                int mx = e.getX(), my = e.getY();
                // 改为“严格网格命中”：仅在草坪内点击才映射到(row,col)
                int[] rc = GamePanel.Grid.pointToGrid(mx, my);
                if (rc == null) {
                    // 场外点击直接忽略，避免被四舍五入到边界格导致错位
                    System.out.println("[放置] 点击不在草坪范围内，已忽略");
                    return;
                }
                int row = rc[0], col = rc[1];

                if (interactionMode == InteractionMode.SHOVEL) {
                    // 铲除模式：左键铲除该格植物
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        boolean removed = gamePanel.removePlantAt(row, col);
                        System.out.println(removed
                                ? "[铲子] 已移除 r" + row + " c" + col + " 的植物"
                                : "[铲子] r" + row + " c" + col + " 无植物可移除");
                    }
                    return;
                }

                // 改用 O(1) 判重，避免每次点击复制列表
                if (gamePanel.hasPlantAt(row, col)) {
                    System.out.println("[放置] 该格已有植物，row=" + row + ", col=" + col);
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    placePlant(row, col, selectedPlantType);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    placePlant(row, col, PlantType.SUNFLOWER);
                }
            }
        });
    }

    // 新增：尝试扣除阳光（不足仅打印小行说明，不弹窗）
    private boolean trySpendSun(int cost) {
        int sun = gamePanel.getSunCount();
        if (sun < cost) {
            System.out.println("[放置] 阳光不足：需要 " + cost + "，当前 " + sun + "（已忽略本次放置）");
            return false;
        }
        gamePanel.setSunCount(sun - cost);
        return true;
    }

    /**
     * placePlant - 将植物实例化并加入面板
     * @param row 网格行（0-based）
     * @param col 网格列（0-based）
     * @param type 植物类型枚举
     */
    private void placePlant(int row, int col, PlantType type) {
        int[] px = GamePanel.Grid.gridToPlantTopLeft(row, col, 70, 90);
        Plant plant = createPlant(type, px[0], px[1], row, col);
        if (plant == null) {
            System.out.println("[放置] 植物创建失败: " + type);
            return;
        }
        int cost = Math.max(0, plant.getCost());
        if (!trySpendSun(cost)) {
            return;
        }
        gamePanel.addPlant(plant);
        System.out.println("[放置] " + plant.getClass().getSimpleName() + " @ r" + row + " c" + col
                + " -> (" + px[0] + "," + px[1] + "), 花费阳光=" + cost
                + "，剩余阳光=" + gamePanel.getSunCount());
    }

    /** 初始化随机刷怪：3~6 秒一个，行随机且避免与上一次相同行 */
    private void initializeZombieSpawner() {
        zombieSpawnTimer = new Timer(randomSpawnDelayMs(), e -> {
            spawnZombieRandomly();
            // 下次间隔重新随机
            zombieSpawnTimer.setDelay(randomSpawnDelayMs());
        });
    }

    private int randomSpawnDelayMs() {
        return 3000 + rng.nextInt(3000); // [3000,6000)
    }

    /** spawnZombieRandomly - 从洗牌袋选择行并创建僵尸（行底对齐） */
    private void spawnZombieRandomly() {
        int row = selectSpawnRow();
        lastSpawnRow = row;
        int zombieH = 100;
        int[] cell = GamePanel.Grid.gridToCellOrigin(row, 0);
        int y = cell[1] + (GamePanel.Grid.CELL_H - zombieH);
        int x = Math.max(1000, gamePanel.getWidth() - 80); // 右侧入场
        Zombie z = new Zombie(x, y, row);
        gamePanel.addZombie(z);
        System.out.println("[刷怪] 随机僵尸 @ row=" + row + " -> (" + x + "," + y + ")");
    }

    // 新增：对外公开的随机生成接口（按钮/脚本可直接调用）
    public void spawnZombieRandom() {
        spawnZombieRandomly();
    }

    // 新增：手动按指定行生成僵尸（对齐行底，统一入口）
    public void spawnZombieInRow(int row) {
        if (gamePanel == null) return;
        row = Math.max(0, Math.min(GamePanel.Grid.ROWS - 1, row));
        int zombieH = 100;
        int[] cell = GamePanel.Grid.gridToCellOrigin(row, 0);
        int y = cell[1] + (GamePanel.Grid.CELL_H - zombieH);
        int x = Math.max(1000, gamePanel.getWidth() - 80);
        Zombie z = new Zombie(x, y, row);
        gamePanel.addZombie(z);
        System.out.println("[刷怪-手动] 在第 " + row + " 行生成僵尸 -> (" + x + "," + y + ")");
    }

    // === 新：从“洗牌袋”中选择一行，优先拥挤度最低的行 ===
    private int selectSpawnRow() {
        if (rowBag.isEmpty()) {
            refillRowBag();
        }
        // 统计当前各行僵尸数（拥挤度）
        int[] counts = new int[GamePanel.Grid.ROWS];
        for (Zombie z : gamePanel.getZombies()) {
            int r = Math.max(0, Math.min(GamePanel.Grid.ROWS - 1, z.getRow()));
            counts[r]++;
        }
        // 在袋中挑选拥挤度最小的行（平局随机，因袋已洗牌）
        int bestRow = -1;
        int bestCount = Integer.MAX_VALUE;
        for (Integer r : rowBag) {
            int c = counts[r];
            if (c < bestCount) {
                bestCount = c;
                bestRow = r;
                if (bestCount == 0) break; // 最优，无需继续
            }
        }
        if (bestRow < 0) {
            // 理论不达，此处兜底：取袋头
            bestRow = rowBag.peekFirst();
        }
        // 从袋中移除所选行
        rowBag.remove(bestRow);
        return bestRow;
    }

    // === 新：补充行袋，按 0..ROWS-1 洗牌后加入 ===
    private void refillRowBag() {
        java.util.List<Integer> rows = new java.util.ArrayList<>(GamePanel.Grid.ROWS);
        for (int i = 0; i < GamePanel.Grid.ROWS; i++) rows.add(i);
        java.util.Collections.shuffle(rows, rng);
        rowBag.clear();
        rowBag.addAll(rows);
    }

    /** 开始游戏（启动计时器） */
    public void startGame() {
        if (!isRunning) {
            isRunning = true;
            gameTimer.start();
            // 重置洗牌袋，确保分布均衡
            refillRowBag();
            if (zombieSpawnTimer != null) zombieSpawnTimer.start();
            System.out.println("游戏开始！");
        }
    }


    /** 暂停游戏（停止计时器但保留状态） */
    public void pauseGame() {
        if (isRunning) {
            isRunning = false;
            gameTimer.stop();
            // 新增：暂停刷怪
            if (zombieSpawnTimer != null) zombieSpawnTimer.stop();
            System.out.println("游戏暂停");
        }
    }

    /** 恢复游戏（从暂停恢复） */
    public void resumeGame() {
        if (!isRunning) {
            isRunning = true;
            gameTimer.start();
            // 新增：恢复刷怪
            if (zombieSpawnTimer != null) zombieSpawnTimer.start();
            System.out.println("游戏继续");
        }
    }

    /** 停止游戏（停止计时器并置为非运行） */
    public void stopGame() {
        isRunning = false;
        gameTimer.stop();
        // 新增：停止刷怪
        if (zombieSpawnTimer != null) zombieSpawnTimer.stop();
        System.out.println("游戏停止");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setSelectedPlantType(PlantType type) {
        if (type != null) this.selectedPlantType = type;
        System.out.println("[选择植物] " + this.selectedPlantType);
    }

    // 新增：设置交互模式
    public void setInteractionMode(InteractionMode mode) {
        if (mode != null) {
            this.interactionMode = mode;
            System.out.println("[交互模式] " + mode);
        }
    }

    /**
     * 统一的植物创建入口：
     * - 优先根据枚举类型创建；
     * - Peashooter 使用反射（若类缺失则回退 Sunflower，保证功能可用）。
     */
    private Plant createPlant(PlantType type, int x, int y, int row, int col) {
        switch (type) {
            case SUNFLOWER:
                return new Sunflower(x, y, row, col);
            case PEASHOOTER:
            default:
                // 通过反射尝试创建 Peashooter，避免编译期依赖
                Plant shooter = createPlantByName("model.entities.Peashooter", x, y, row, col);
                if (shooter != null) return shooter;
                System.out.println("[放置] 未找到 Peashooter 类，回退为 Sunflower");
                return new Sunflower(x, y, row, col);
        }
    }

    /**
     * 反射创建指定全类名的植物实例（签名为 (int x, int y, int row, int col)）
     * 缺失/失败时返回 null。
     */
    private Plant createPlantByName(String fqcn, int x, int y, int row, int col) {
        try {
            Class<?> cls = Class.forName(fqcn);
            java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor(int.class, int.class, int.class, int.class);
            Object obj = ctor.newInstance(x, y, row, col);
            if (obj instanceof Plant) return (Plant) obj;
        } catch (Throwable ignore) {
            // 安静回退
        }
        return null;
    }
}
