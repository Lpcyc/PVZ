//import view.GamePanel;
//import controller.GameController;
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import view.asset.AssetLoader;
//import javax.swing.event.ChangeListener;
//
///**
// * Main - 应用入口与 GUI 组装
// *
// * 职责说明：
// * - 在 EDT 启动并组装 Swing GUI（确保线程安全）；
// * - 构造 GamePanel、GameController 并连接控制面板事件；
// * - 提供启动/暂停/恢复的联动（包括对资源动画的全局 pause/resume）；
// * - 提供轻量的状态输出接口用于运行时诊断（非测试用例）。
// *
// * 线程约定：
// * - 所有 Swing 组件的创建与交互必须在 EDT 中执行；
// * - AssetLoader 的长时间 I/O 操作应由其自身异步机制处理，Main 仅发起控制命令。
// *
// * 可改进点（记录）：
// * - 将 refreshAnimatedAssets 的资源键列表配置化，避免硬编码；
// * - 将状态输出改为可插拔的 Logger（当前用 System.out 作轻量诊断）。
// */
//public class Main {
//    private static GameController gameController;
//    private static JButton startButton;
//    private static JButton pauseButton;
//    private static JLabel statusLabel;
//    public static void main(String[] args) {
//        // 在事件分发线程中创建GUI
//        initGlobalErrorHandlers();
//
//        SwingUtilities.invokeLater(() -> {
//            if (!GraphicsEnvironment.isHeadless()) {
//                createAndShowGUI();
//            } else {
//                System.err.println("当前环境为 headless，无法创建 Swing 界面。");
//            }
//        });
//    }
//
//    /** 创建窗口与布局：说明放在方法注释中，便于维护 */
//    private static void createAndShowGUI() {
//        // 创建主窗口
//        JFrame frame = new JFrame("植物大战僵尸");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setLayout(new BorderLayout());
//
//        // 创建游戏面板
//        GamePanel gamePanel = new GamePanel();
//        frame.add(gamePanel, BorderLayout.CENTER);
//
//        // 创建控制面板（传入 gamePanel，便于事件中添加僵尸）
//        JPanel controlPanel = createControlPanel(gamePanel);
//        frame.add(controlPanel, BorderLayout.SOUTH);
//        //创建状态面板（顶部并排显示植物选择）
//        JPanel statusPanel = createStatusPanel();
//        frame.add(statusPanel,BorderLayout.NORTH);
//        // 创建游戏控制器
//        gameController = new GameController(gamePanel);
//
//        // 设置窗口属性
//        frame.pack();
//        frame.setResizable(false); // 固定 1200x600 视口，防止缩放
//        frame.setLocationRelativeTo(null);
//        frame.setVisible(true);
//
//        System.out.println("游戏界面加载完成！点击开始按钮启动游戏。");
//    }
//
//    /**
//     * 控制面板（开始/暂停/滚动背景等占位按钮）
//     * - 启动后交由 GameController 的 Swing Timer 驱动 updateGame。
//     */
//    private static JPanel createControlPanel(GamePanel gamePanel) {
//        JPanel panel = new JPanel(new FlowLayout());
//
//        startButton = new JButton("开始游戏");
//        pauseButton = new JButton("暂停游戏");
//        pauseButton.setEnabled(false);
//        JButton spawnButton = new JButton("生成僵尸");
//        JButton statusButton = new JButton("输出状态");
//
//        startButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                gameController.startGame();
//                AssetLoader.getInstance().resumeAllAnimations();
//                // 新增：恢复动画后强制刷新常用动图资源，修复暂停后 GIF 不动的问题
//                refreshAnimatedAssets();
//                startButton.setEnabled(false);
//                pauseButton.setEnabled(true);
//                // 修改：开始后随机生成一个僵尸，触发演示（非固定第四行/豌豆同行）
//                if (gameController != null) gameController.spawnZombieRandom();
//                printProjectStatus(gamePanel);
//            }
//        });
//
//        pauseButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (gameController.isRunning()) {
//                    gameController.pauseGame();
//                    // 新增：全局暂停动画
//                    AssetLoader.getInstance().pauseAllAnimations();
//                    pauseButton.setText("继续游戏");
//                } else {
//                    gameController.resumeGame();
//                    // 新增：全局恢复动画
//                    AssetLoader.getInstance().resumeAllAnimations();
//                    // 新增：恢复后刷新常用动图资源，避免仍是暂停时的静态快照
//                    refreshAnimatedAssets();
//                    pauseButton.setText("暂停游戏");
//                }
//            }
//        });
//
//        // 手动生成僵尸（使用控制器的随机生成）
//        spawnButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (gameController != null) gameController.spawnZombieRandom();
//            }
//        });
//
//        // 新增：输出当前状态与资源简报
//        statusButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                printProjectStatus(gamePanel);
//            }
//        });
//
//        panel.add(startButton);
//        panel.add(pauseButton);
//        panel.add(spawnButton);
//        panel.add(statusButton); // 新增
//
//        // 新增：显示坐标线（仅画线，不显示坐标映射）
//        JCheckBox gridToggle = new JCheckBox("显示坐标线", false);
//        gridToggle.addActionListener(e -> gamePanel.setShowGridOverlay(gridToggle.isSelected()));
//        panel.add(gridToggle);
//
//        return panel;
//    }
//
//    // 状态栏（文本更新示例）
//    private static JPanel createStatusPanel() {
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.setBackground(new Color(240, 240, 240));
//        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
//
//        // 左侧状态
//        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
//        left.setOpaque(false);
//        JLabel titleLabel = new JLabel("游戏状态:");
//        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
//
//        statusLabel = new JLabel("就绪");
//        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
//        statusLabel.setForeground(Color.BLUE);
//
//        left.add(titleLabel);
//        left.add(statusLabel);
//
//        // 中部：植物选项卡（点击即选中，随后直接左键点草坪格放置；新增“铲子”）
//        JTabbedPane plantTabs = new JTabbedPane(JTabbedPane.TOP);
//        plantTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
//        // 简单内容占位（可替换为卡片图标或介绍）
//        JPanel peaTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
//        peaTab.add(new JLabel("豌豆射手：远程攻击，自动对同行前方僵尸射击"));
//        JPanel sunTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
//        sunTab.add(new JLabel("向日葵：周期性产出阳光"));
//        // 新增：铲子说明
//        JPanel shovelTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
//        shovelTab.add(new JLabel("铲子：点击草坪格即可移除该格的植物"));
//
//        plantTabs.addTab("Peashooter", null, peaTab, "点击选择 Peashooter，再在草坪网格左键放置");
//        plantTabs.addTab("Sunflower", null, sunTab, "点击选择 Sunflower，再在草坪网格左键放置");
//        plantTabs.addTab("铲子", null, shovelTab, "点击选择铲子，再点击草坪网格移除该格植物");
//        plantTabs.setSelectedIndex(0); // 默认选中豌豆
//
//        // 切换选项卡时更新当前交互模式/植物类型
//        plantTabs.addChangeListener((ChangeListener) e -> {
//            int idx = plantTabs.getSelectedIndex();
//            if (gameController != null) {
//                if (idx == 2) {
//                    gameController.setInteractionMode(GameController.InteractionMode.SHOVEL);
//                    if (statusLabel != null) statusLabel.setText("已选择: 铲子（点击格子可移除植物）");
//                } else {
//                    gameController.setInteractionMode(GameController.InteractionMode.PLACE);
//                    GameController.PlantType type = (idx == 1)
//                            ? GameController.PlantType.SUNFLOWER
//                            : GameController.PlantType.PEASHOOTER;
//                    gameController.setSelectedPlantType(type);
//                    if (statusLabel != null) {
//                        statusLabel.setText("已选择: " + (type == GameController.PlantType.SUNFLOWER ? "Sunflower" : "Peashooter"));
//                    }
//                }
//            }
//        });
//
//        panel.add(left, BorderLayout.WEST);
//        panel.add(plantTabs, BorderLayout.CENTER);
//        return panel;
//    }
//
//    // ===== 新增：全局异常处理 =====
//    private static void initGlobalErrorHandlers() {
//        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
//            System.err.println("[未捕获异常] 线程: " + t.getName());
//            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, "未捕获异常，线程: " + t.getName(), e);
//            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
//                    null,
//                    "发生未捕获异常: " + e.getClass().getSimpleName() + "\n" + e.getMessage(),
//                    "错误",
//                    JOptionPane.ERROR_MESSAGE
//            ));
//        });
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                try {
//                    AssetLoader rl = AssetLoader.getInstance();
//                    if (rl != null) rl.unloadAll();
//                } catch (Exception ignore) {
//                }
//                System.out.println("应用退出。");
//            } catch (Throwable t) {
//                System.err.println("ShutdownHook 异常: " + t.getMessage());
//            }
//        }, "ShutdownHook"));
//    }
//
//   //一次性状态输出方法
//    private static void printProjectStatus(GamePanel gamePanel) {
//        if (gamePanel == null) return;
//        System.out.print(gamePanel.getStatusReport());
//
//        Runtime rt = Runtime.getRuntime();
//        long max = rt.maxMemory() / 1024;
//        long total = rt.totalMemory() / 1024;
//        long free = rt.freeMemory() / 1024;
//        long used = total - free;
//        System.out.println(String.format("JVM 内存: used=%d KB, free=%d KB, total=%d KB, max=%d KB", used, free, total, max));
//        System.out.println("时间: " + new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date()));
//        System.out.println("--------------------");
//    }
//
//    /**
//     * 恢复动画后刷新常用动画资源
//     *
//     * 说明：
//     * - 目的是修复暂停/恢复后 GIF 仍为静态快照的问题；
//     * - 采用“卸载→加载”策略，代价是短暂的解码开销。可改为更细粒度刷新以优化性能。
//     */
//    private static void refreshAnimatedAssets() {
//        AssetLoader loader = AssetLoader.getInstance();
//        // 常用动图键（可按需扩展）
//        String[] keys = new String[] {
//                "plant_peashooter",
//                "plant_sunflower",
//                "zombie_basic",
//                // 若项目中使用目录序列或额外键，可加入以下备用键
//                "zombie_normal_walk_dir",
//                "plant_peashooter_idle_dir",
//                "plant_sunflower_idle_dir"
//        };
//        for (String k : keys) {
//            try { loader.unloadImage(k); } catch (Exception ignore) {}
//            try { loader.loadImage(k); } catch (Exception ignore) {}
//        }
//    }
//}
//
//// 本次与实体架构相关改动无需调整 Main，保留原有自检与GUI逻辑
