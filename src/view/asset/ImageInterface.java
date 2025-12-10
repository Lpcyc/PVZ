/*
 ImageInterface
 统一图片与动画的最小操作集合。
 说明:
   - 所有实现需保证 load/unload 的幂等性（重复调用不抛异常）。
   - draw 系列方法应在资源未就绪时安全返回（不抛出）。
*/
package view.asset;

import javax.swing.*;
import java.awt.*;

public interface ImageInterface {
    String getKey();                        // 资源唯一标识
    String getPath();                       // 类路径或绝对路径
    int getWidth();                         // 宽（未加载可为 -1）
    int getHeight();                        // 高（未加载可为 -1）
    boolean isLoaded();                     // 是否已解码完成

    Image getImage();                       // 底层 AWT 图像（可能 null）
    ImageIcon getImageIcon();               // Swing 图标（动画或静态）
    Image getScaledImage(int width,int height); // 缩放副本（不修改原图）

    boolean isAnimated();                   // 是否为动画类型
    int getFrameCount();                    // 帧数（静态=1）
    double getFrameRate();                  // 帧率（静态=0）

    void play();                            // 开始/继续播放
    void pause();                           // 暂停播放
    void stop();                            // 停止并复位
    boolean isPlaying();                    // 当前播放状态

    void draw(Graphics g,int x,int y);                      // 原尺寸绘制
    void draw(Graphics g,int x,int y,int width,int height); // 指定区域绘制（可缩放）

    boolean load();                         // 加载资源
    void unload();                          // 卸载资源
    void dispose();                         // 释放（语义同 unload）
}
