/*
 功能说明（BackgroundRenderer）
 - 只构建一次性的背景缓存层（BufferedImage），随后每帧直接 blit 到面板，减少重复解码/缩放开销。
 - 仅绘制左上原始像素区域（不拉伸到全宽），不足区域填充纯色。

 具体改进建议
 1) 缓存有效性：当底层资源重新加载（AssetLoader.reloadImage）或主题更换时，应有回调机制通知 BackgroundRenderer.invalidateCache()。
 2) 线程：ensureCachedLayer 在构建时尽量避免直接在 EDT 做 I/O；当前实现用 assetLoader.getAwImage（会异步触发加载）是可行的，但应确保第一次构建不阻塞。
 3) 高 DPI 支持：考虑设备缩放因子（Java DPI 缩放）以保证在高分辨率屏幕上显示清晰。
 4) 日志清理：将 System.err.println 换成 Logger，减少生产环境噪音。
 5) 可配置：允许背景绘制策略切换（原始像素/拉伸/平铺）。
*/
package view.renderers;

import view.asset.AssetLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BackgroundRenderer {
    private static final String BG_KEY = "main_lawn";
    private static final String BG_PATH = "/backgrounds/main_lawn.png";
    private static final Color FILL_COLOR = new Color(60, 160, 60);

    private final AssetLoader assetLoader;
    private volatile BufferedImage cachedLayer;
    private int cachedW = -1, cachedH = -1;

    public BackgroundRenderer(AssetLoader loader) {
        this.assetLoader = loader;
        loadBackgroundImage();
    }

    /**
     * 首次调用时触发背景资源加载（同步），失败则后续渲染用纯色填充。
     */
    private void loadBackgroundImage() {
        assetLoader.loadImage(BG_KEY, BG_PATH);
    }

    /**
     * 渲染背景：优先使用缓存层，无则重建。
     *
     * @param g 画笔（EDT 内）
     */
    public void render(Graphics g, JComponent panel) {
        if (panel == null || g == null) return;
        int pw = panel.getWidth(), ph = panel.getHeight();
        if (pw <= 0 || ph <= 0) return;
        BufferedImage layer = ensureCachedLayer(panel);
        if (layer != null) {
            g.drawImage(layer, 0, 0, null);
        } else {
            g.setColor(FILL_COLOR);
            g.fillRect(0, 0, pw, ph);
        }
    }

    /**
     * 确保缓存层有效：尺寸变化或尚未构建时重建 BufferedImage。
     * 注意：
     * - 本方法会调用 AssetLoader.getAwImage，若资源仍未解码将返回 null；
     * - 构建缓存时应尽量在 EDT 短时间内完成绘制，避免做大量同步 I/O。
     */
    private BufferedImage ensureCachedLayer(JComponent panel) {
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());
        if (cachedLayer != null && w == cachedW && h == cachedH) return cachedLayer;
        try {
            BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = buf.createGraphics();
            try {
                g2.setColor(FILL_COLOR);
                g2.fillRect(0, 0, w, h);
                Image awt = assetLoader.getAwImage(BG_KEY);
                if (awt != null) {
                    int iw = awt.getWidth(panel), ih = awt.getHeight(panel);
                    if (iw > 0 && ih > 0) {
                        int sw = Math.min(iw, w), sh = Math.min(ih, h);
                        g2.drawImage(awt, 0, 0, sw, sh, 0, 0, sw, sh, panel);
                    }
                }
            } finally {
                g2.dispose();
            }
            cachedLayer = buf;
            cachedW = w; cachedH = h;
        } catch (Throwable t) {
            cachedLayer = null;
            cachedW = cachedH = -1;
        }
        return cachedLayer;
    }

    /**
     * invalidateCache - 主动使缓存失效
     * 使用场景：
     * - 资源被重新加载/替换（主题切换）时调用；
     * - 避免长时间使用过时的缓存。
     */
    public void invalidateCache() {
        cachedLayer = null;
        cachedW = cachedH = -1;
    }
}
