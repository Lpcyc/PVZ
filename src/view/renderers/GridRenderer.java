package view.renderers;

import view.GridConverter;

import java.awt.*;

/**
 * GridRenderer - 网格调试渲染器
 *
 * 说明：
 * - linesOnly=true 时仅绘制网格线（用于临时对齐调试），不绘制草坪底色或坐标标签；
 * - 非运行时组件，仅用于开发时视觉辅助。
 *
 * 用途：
 * - 独立绘制草坪底色、网格线与(列,行)标签；
 * - 与 GamePanel 的固定网格参数一致（9x5, 83x100, 起点 250,75）。
 * 注意：
 * - 非 GamePanel 的正式渲染路径，仅用于开发调试辅助；
 * - 生产逻辑应以 GamePanel 内的 drawGrid / drawFieldCoordinates 为准。
 */
public class GridRenderer {
    // Use the central GridConverter for all grid parameters
    private static final int GRID_ROWS = 5; // Assuming 5 rows
    private static final int GRID_COLS = 9; // Assuming 9 columns
    private static final int GRID_CELL_WIDTH = GridConverter.CELL_WIDTH;
    private static final int GRID_CELL_HEIGHT = GridConverter.CELL_HEIGHT;
    private static final int LAWN_START_X = GridConverter.GRID_START_X;
    private static final int LAWN_START_Y = GridConverter.GRID_START_Y;

    // 新增：仅线模式（true 时只绘制网格线，不绘制草坪底色与坐标标签）
    private boolean linesOnly = true;
    public void setLinesOnly(boolean linesOnly) { this.linesOnly = linesOnly; }

    /** 入口渲染：绘制草坪底色、网格线与标签 */
    public void render(Graphics g) {
        if (!linesOnly) {
            drawLawnBackground(g);
        }
        drawGridLines(g);
        if (!linesOnly) {
            drawGridLabels(g);
        }
    }

    /** 草坪底色（半透明） */
    private void drawLawnBackground(Graphics g) {
        g.setColor(new Color(100, 200, 100, 150));
        g.fillRect(LAWN_START_X, LAWN_START_Y,
                GRID_COLS * GRID_CELL_WIDTH,
                GRID_ROWS * GRID_CELL_HEIGHT);
    }

    /** 网格线（行/列） */
    private void drawGridLines(Graphics g) {
        g.setColor(new Color(80, 180, 80));

        // 绘制行线
        for (int row = 0; row <= GRID_ROWS; row++) {
            int y = LAWN_START_Y + row * GRID_CELL_HEIGHT;
            g.drawLine(LAWN_START_X, y,
                    LAWN_START_X + GRID_COLS * GRID_CELL_WIDTH, y);
        }

        // 绘制列线
        for (int col = 0; col <= GRID_COLS; col++) {
            int x = LAWN_START_X + col * GRID_CELL_WIDTH;
            g.drawLine(x, LAWN_START_Y,
                    x, LAWN_START_Y + GRID_ROWS * GRID_CELL_HEIGHT);
        }
    }

    /** 单元标签（列,行，0-based） */
    private void drawGridLabels(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = LAWN_START_X + col * GRID_CELL_WIDTH + 5;
                int y = LAWN_START_Y + row * GRID_CELL_HEIGHT + 15;
                g.drawString(col + "," + row, x, y);
            }
        }
    }
}