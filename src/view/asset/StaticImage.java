/*
 StaticImage
 单帧静态图片实现：PNG/JPG/GIF(首帧)。
 - load(): 解码并缓存 ImageIcon
 - unload(): flush 释放图像内存
 - getBufferedImage(): 若底层是 BufferedImage 可直接返回便于像素操作
*/
package view.asset;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class StaticImage extends AbstractImage {
    public StaticImage(String key, String path) { super(key, path); }

    /**
     * 加载静态图片。
     * @return true 成功 false 失败（路径不存在或格式异常）
     */
    @Override
    public boolean load() {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return false;
            imageIcon = new ImageIcon(url);
            finalizeDimensions();
            loaded = true;
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 卸载静态图片（安全幂等）。
     */
    @Override
    public void unload() {
        if (imageIcon != null) {
            Image im = imageIcon.getImage();
            if (im != null) im.flush();
        }
        imageIcon = null;
        loaded = false;
        width = -1;
        height = -1;
    }

    @Override
    public boolean isAnimated() { return false; }

    /**
     * 若内部 Image 为 BufferedImage 则返回，方便外部做像素处理或截图。
     */
    public BufferedImage getBufferedImage() {
        Image im = getImage();
        return (im instanceof BufferedImage) ? (BufferedImage) im : null;
    }
}