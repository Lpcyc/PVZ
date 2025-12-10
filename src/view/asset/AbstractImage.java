/*
 AbstractImage
 职责:
   - 所有图片资源（静态/动画）的抽象父类，提供统一的 key/path/加载状态/尺寸缓存/基本绘制能力。
 线程与调用约定:
   - load()/unload()：可在后台线程调用；建议避免在 EDT 中做大量 I/O。
   - draw(...)：必须在 EDT（Swing 渲染线程）调用。
 失败与回退:
   - 若未加载成功，draw 会绘制占位灰块，不抛异常。
 字段说明:
   key     唯一资源标识（逻辑名称）
   path    类路径或绝对路径（统一为类路径“/”起始）
   loaded  是否已成功加载并可安全绘制
   width/height  缓存的像素尺寸（首次通过 ImageIcon 延迟填充，未加载时为 -1）
 性能注意:
   - 多次 draw 不会重复计算尺寸；finalizeDimensions() 在子类 load 成功后调用以锁定尺寸。
 扩展建议:
   - 子类若需额外的元数据（如帧率、帧数）可添加字段并在 load() 中填充。
*/
package view.asset;

import javax.swing.*;
import java.awt.*;

/**
 * 抽象图片基础类型：定义公共接口的默认部分实现。
 */
public abstract class AbstractImage implements ImageInterface {
    protected final String key;
    protected final String path;
    protected ImageIcon imageIcon;
    protected boolean loaded = false;
    protected int width = -1;
    protected int height = -1;

    public AbstractImage(String key, String path) {
        this.key = key == null ? "" : key;
        this.path = path == null ? "" : path;
    }

    /**
     * 获取资源唯一标识（不为 null）。
     * @return key 字符串（若构造时传 null 则为空字符串）
     */
    @Override
    public String getKey() { return key; }

    /**
     * 获取资源路径（类路径或绝对路径）。
     * @return 原始或规范化路径（可能为空字符串）
     */
    @Override
    public String getPath() { return path; }

    /**
     * 是否已加载完成。
     * @return true 表示可安全绘制；false 表示尚未或加载失败
     */
    @Override
    public boolean isLoaded() { return loaded; }

    /**
     * 获取宽度（懒加载填充）。
     * @return >=0 表示已知宽度，-1 表示未知或未加载
     */
    @Override
    public int getWidth() {
        if (width == -1 && imageIcon != null) width = imageIcon.getIconWidth();
        return width;
    }

    /**
     * 获取高度（懒加载填充）。
     * @return >=0 表示已知高度，-1 表示未知或未加载
     */
    @Override
    public int getHeight() {
        if (height == -1 && imageIcon != null) height = imageIcon.getIconHeight();
        return height;
    }

    /**
     * 返回底层 AWT Image（可能为 null）。
     * @return Image 或 null
     */
    @Override
    public Image getImage() {
        return imageIcon != null ? imageIcon.getImage() : null;
    }

    /**
     * 返回 ImageIcon（动画或静态），未加载时可能为 null。
     */
    @Override
    public ImageIcon getImageIcon() { return imageIcon; }

    /**
     * 获取缩放后的副本（不缓存），用于一次性快速缩放。
     * @param w 目标宽度（<=0 返回 null）
     * @param h 目标高度（<=0 返回 null）
     * @return 缩放后 Image 或 null
     */
    @Override
    public Image getScaledImage(int w, int h) {
        if (w <= 0 || h <= 0) return null;
        Image img = getImage();
        return (img != null) ? img.getScaledInstance(w, h, Image.SCALE_SMOOTH) : null;
    }

    /**
     * 原尺寸绘制（若未加载则绘制占位块）。
     * @param g 图形上下文（必须非 null）
     * @param x 左上角 X
     * @param y 左上角 Y
     */
    @Override
    public void draw(Graphics g, int x, int y) {
        if (g == null) return;
        if (imageIcon != null) {
            imageIcon.paintIcon(null, g, x, y);
        } else {
            // 占位绘制（轻量防御）
            g.setColor(new Color(200, 200, 200));
            g.fillRect(x, y, 32, 32);
        }
    }

    /**
     * 指定尺寸绘制（若获取不到原图则绘制占位块）。
     * @param g 图形上下文
     * @param x 左上角 X
     * @param y 左上角 Y
     * @param w 目标宽
     * @param h 目标高
     */
    @Override
    public void draw(Graphics g, int x, int y, int w, int h) {
        if (g == null || w <= 0 || h <= 0) return;
        Image img = getScaledImage(w, h);
        if (img != null) {
            g.drawImage(img, x, y, w, h, null);
        } else {
            g.setColor(new Color(180, 180, 180));
            g.fillRect(x, y, w, h);
        }
    }

    // 默认：静态图
    @Override public boolean isAnimated() { return false; }
    @Override public int getFrameCount() { return 1; }
    @Override public double getFrameRate() { return 0; }
    @Override public void play() {}
    @Override public void pause() {}
    @Override public void stop() {}
    @Override public boolean isPlaying() { return false; }

    /**
     * 释放资源（语义同 unload，可被上层统一调用）。
     */
    @Override
    public void dispose() { unload(); }

    /**
     * 子类在加载成功后调用，确保宽高被写入缓存，避免重复查询。
     */
    protected void finalizeDimensions() {
        if (imageIcon != null) {
            this.width = imageIcon.getIconWidth();
            this.height = imageIcon.getIconHeight();
        }
    }

    // 子类必须实现
    @Override public abstract boolean load();
    @Override public abstract void unload();
}
