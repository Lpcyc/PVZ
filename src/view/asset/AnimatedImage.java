/*
 AnimatedImage
 支持来源:
   1. GIF 动画文件（后缀 .gif）
   2. 序列清单 .seq（可包含首行 fps=XX，后续为帧路径）
   3. 目录序列（路径以 / 或 /* 结尾，自动枚举目录内图片帧）
 特点:
   - 一次性加载所有帧（序列），基于系统时间计算当前帧索引
   - GIF 简化为 frameCount + 固定回退帧率（避免复杂元数据开销）
 线程:
   - load/unload 可在后台线程调用
   - draw 必须在 EDT
 同步:
   - 关键状态受 lock 保护，避免并发访问 seqIcons 或播放指针时不一致
 回退:
   - 加载失败返回 false，不抛异常；外部可检测 isLoaded()
*/
package view.asset;

import javax.swing.*;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.JarURLConnection;

public class AnimatedImage extends AbstractImage {
    private volatile boolean playing = true;
    private volatile boolean actuallyAnimated = false;
    private List<ImageIcon> seqIcons;
    private int seqFps = 12;
    private long playStartMs = 0L;
    private long pausedElapsedMs = 0L;
    private int gifFrameCount = 1;
    private double gifFps = 0.0;
    private boolean isGif = false;
    private int pausedFrameIndex = 0;
    private final Object lock = new Object();

    /**
     * 构造函数
     * @param key 资源唯一标识
     * @param path 类路径（序列/目录/GIF）
     */
    public AnimatedImage(String key, String path) { super(key, path); }

    /**
     * 加载资源（自动判别类型）
     * @return true 成功 false 失败
     */
    @Override
    public boolean load() {
        synchronized (lock) {
            if (loaded || path == null) return loaded;
            String lower = path.toLowerCase(Locale.ROOT);
            boolean ok =
                    lower.endsWith(".seq") ? loadSeqManifest(path) :
                    isDirectorySequencePath(path) ? loadSeqDirectory(path) :
                    loadGif(path);
            if (!ok) return false;
            loaded = true;
            playStartMs = System.currentTimeMillis();
            pausedElapsedMs = 0L;
            playing = true;
            return true;
        }
    }

    /**
     * 卸载并释放所有帧图像，允许后续重新加载。
     */
    @Override
    public void unload() {
        synchronized (lock) {
            stopInternal();
            if (seqIcons != null) {
                for (ImageIcon ic : seqIcons) {
                    Image im = ic.getImage();
                    if (im != null) im.flush();
                }
                seqIcons.clear();
                seqIcons = null;
            }
            if (imageIcon != null) {
                Image im = imageIcon.getImage();
                if (im != null) im.flush();
            }
            imageIcon = null;
            width = -1;
            height = -1;
            loaded = false;
            actuallyAnimated = false;
            gifFrameCount = 1;
            gifFps = 0.0;
            isGif = false;
            pausedFrameIndex = 0;
        }
    }

    /**
     * 加载 GIF 文件，初始化 imageIcon / 宽高 / 帧数。
     */
    private boolean loadGif(String gifPath) {
        try {
            URL url = getClass().getResource(gifPath);
            if (url == null) return false;
            imageIcon = new ImageIcon(url);
            finalizeDimensions();
            isGif = true;
            gifFrameCount = estimateGifFrames(url);
            gifFps = gifFrameCount > 1 ? 10.0 : 0.0;
            actuallyAnimated = gifFrameCount > 1;
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 粗略估算 GIF 帧数（使用 ImageReader），降低解析复杂度。
     */
    private int estimateGifFrames(URL url) {
        int frames = 1;
        try (ImageInputStream iis = ImageIO.createImageInputStream(url.openStream())) {
            if (iis == null) return 1;
            Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return 1;
            javax.imageio.ImageReader reader = readers.next();
            try {
                reader.setInput(iis, false, false);
                frames = reader.getNumImages(true);
            } finally {
                reader.dispose();
            }
        } catch (Exception ignore) {}
        return Math.max(1, frames);
    }

    /**
     * 加载 .seq 清单格式：可解析 fps= 行与后续帧列表。
     */
    private boolean loadSeqManifest(String seqPath) {
        URL in = getClass().getResource(seqPath);
        if (in == null) return false;
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (!s.isEmpty() && !s.startsWith("#")) lines.add(s);
            }
        } catch (Exception e) {
            return false;
        }
        if (lines.isEmpty()) return false;
        int idx = 0;
        if (lines.get(0).toLowerCase(Locale.ROOT).startsWith("fps=")) {
            try {
                int f = Integer.parseInt(lines.get(0).substring(4).trim());
                if (f > 0 && f <= 240) seqFps = f;
            } catch (Exception ignore) {}
            idx = 1;
        }
        String baseDir = seqPath.substring(0, seqPath.lastIndexOf('/') + 1);
        List<ImageIcon> icons = new ArrayList<>();
        int fw = -1, fh = -1;
        for (int i = idx; i < lines.size(); i++) {
            String p = lines.get(i);
            String resolved = p.startsWith("/") ? p : (baseDir + p);
            URL u = getClass().getResource(resolved);
            if (u == null) continue;
            ImageIcon ic = new ImageIcon(u);
            icons.add(ic);
            if (fw < 0) {
                fw = ic.getIconWidth();
                fh = ic.getIconHeight();
            }
        }
        if (icons.isEmpty()) return false;
        seqIcons = icons;
        width = fw;
        height = fh;
        actuallyAnimated = seqIcons.size() > 1;
        return true;
    }

    /**
     * 加载目录序列（顺序由 compareNames 决定：数字自然排序优先）。
     */
    private boolean loadSeqDirectory(String rawDir) {
        String dir = normalizeDirPath(rawDir);
        List<String> entries = collectDirectoryEntries(dir);
        if (entries.isEmpty()) return false;
        entries.sort(this::compareNames);
        List<ImageIcon> icons = new ArrayList<>();
        int fw = -1, fh = -1;
        for (String res : entries) {
            URL u = getClass().getResource(res);
            if (u == null) continue;
            ImageIcon ic = new ImageIcon(u);
            icons.add(ic);
            if (fw < 0) {
                fw = ic.getIconWidth();
                fh = ic.getIconHeight();
            }
        }
        if (icons.isEmpty()) return false;
        seqIcons = icons;
        width = fw;
        height = fh;
        actuallyAnimated = seqIcons.size() > 1;
        return true;
    }

    /**
     * 收集目录中的图片资源（支持 file/jar）。
     */
    private List<String> collectDirectoryEntries(String dir) {
        List<String> names = new ArrayList<>();
        try {
            URL dirUrl = getClass().getResource(dir);
            if (dirUrl != null && "file".equalsIgnoreCase(dirUrl.getProtocol())) {
                java.io.File df = new java.io.File(dirUrl.toURI());
                java.io.File[] files = df.listFiles(f -> f.isFile() && isImageExt(f.getName()));
                if (files != null) {
                    Arrays.sort(files, (a, b) -> compareNames(a.getName(), b.getName()));
                    for (java.io.File f : files) names.add((dir + f.getName()).replace("//", "/"));
                }
            } else {
                String relDir = dir.startsWith("/") ? dir.substring(1) : dir;
                if (dirUrl != null && "jar".equalsIgnoreCase(dirUrl.getProtocol())) {
                    JarURLConnection conn = (JarURLConnection) dirUrl.openConnection();
                    try (JarFile jar = conn.getJarFile()) {
                        scanJarEntries(jar, conn.getEntryName(), names);
                    }
                } else {
                    URL code = getClass().getProtectionDomain().getCodeSource().getLocation();
                    java.io.File codeFile = new java.io.File(code.toURI());
                    if (codeFile.isDirectory()) {
                        java.io.File tryDir = new java.io.File(codeFile, relDir);
                        if (tryDir.isDirectory()) {
                            java.io.File[] files = tryDir.listFiles(f -> f.isFile() && isImageExt(f.getName()));
                            if (files != null) {
                                Arrays.sort(files, (a, b) -> compareNames(a.getName(), b.getName()));
                                for (java.io.File f : files) names.add((dir + f.getName()).replace("//", "/"));
                            }
                        }
                    } else if (codeFile.isFile() && codeFile.getName().endsWith(".jar")) {
                        try (JarFile jar = new JarFile(codeFile)) {
                            scanJarEntries(jar, relDir, names);
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return names;
    }

    /**
     * 遍历 jar 包条目以收集目录内图片。
     */
    private void scanJarEntries(JarFile jar, String baseDir, List<String> out) {
        String prefix = baseDir.endsWith("/") ? baseDir : (baseDir + "/");
        Enumeration<JarEntry> en = jar.entries();
        while (en.hasMoreElements()) {
            JarEntry je = en.nextElement();
            if (je.isDirectory()) continue;
            String name = je.getName();
            if (!name.startsWith(prefix)) continue;
            String file = name.substring(prefix.length());
            if (file.contains("/")) continue;
            if (isImageExt(file)) out.add("/" + name);
        }
    }

    /**
     * 恢复播放（暂停后根据 pausedElapsedMs 续播）。
     */
    @Override
    public void play() {
        synchronized (lock) {
            if (!loaded || playing) return;
            playStartMs = System.currentTimeMillis() - pausedElapsedMs;
            playing = true;
        }
    }

    /**
     * 暂停播放：冻结当前帧并保存时间偏移。
     */
    @Override
    public void pause() {
        synchronized (lock) {
            if (!loaded || !playing) return;
            pausedElapsedMs = elapsedMs();
            pausedFrameIndex = currentFrameIndexUnlocked();
            playing = false;
            if (isGif && imageIcon != null) snapshotFreezeGif();
        }
    }

    /**
     * 冻结 GIF 当前帧为静态快照，避免继续动画。
     */
    private void snapshotFreezeGif() {
        try {
            int w = Math.max(1, imageIcon.getIconWidth());
            int h = Math.max(1, imageIcon.getIconHeight());
            BufferedImage snap = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = snap.createGraphics();
            imageIcon.paintIcon(null, g2, 0, 0);
            g2.dispose();
            imageIcon = new ImageIcon(snap);
        } catch (Exception ignore) {}
    }

    /**
     * 停止播放并复位（归零计时）。
     */
    @Override
    public void stop() {
        synchronized (lock) {
            stopInternal();
        }
    }

    private void stopInternal() {
        playing = false;
        playStartMs = 0L;
        pausedElapsedMs = 0L;
        pausedFrameIndex = 0;
    }

    @Override
    public boolean isPlaying() { return playing; }

    @Override
    public int getFrameCount() {
        if (seqIcons != null) return seqIcons.size();
        return isGif ? gifFrameCount : (actuallyAnimated ? gifFrameCount : 1);
    }

    @Override
    public double getFrameRate() {
        if (seqIcons != null) return Math.max(1, seqFps);
        return isGif ? Math.max(0, gifFps) : (actuallyAnimated ? 10.0 : 0.0);
    }

    @Override
    public boolean isAnimated() {
        if (seqIcons != null) return seqIcons.size() > 1;
        return isGif && gifFrameCount > 1;
    }

    /**
     * 获取当前帧对应的 Image（序列或 GIF）。
     */
    @Override
    public Image getImage() {
        synchronized (lock) {
            if (seqIcons != null && !seqIcons.isEmpty()) {
                int idx = playing ? currentFrameIndexUnlocked() : Math.min(pausedFrameIndex, seqIcons.size() - 1);
                if (idx < 0 || idx >= seqIcons.size()) idx = 0;
                return seqIcons.get(idx).getImage();
            }
            return imageIcon != null ? imageIcon.getImage() : null;
        }
    }

    /**
     * 原尺寸绘制当前帧。
     */
    @Override
    public void draw(Graphics g, int x, int y) {
        synchronized (lock) {
            if (g == null) return;
            if (seqIcons != null && !seqIcons.isEmpty()) {
                int idx = playing ? currentFrameIndexUnlocked() : Math.min(pausedFrameIndex, seqIcons.size() - 1);
                if (idx < 0 || idx >= seqIcons.size()) idx = 0;
                seqIcons.get(idx).paintIcon(null, g, x, y);
                return;
            }
            if (imageIcon != null) imageIcon.paintIcon(null, g, x, y);
        }
    }

    /**
     * 指定尺寸绘制（可缩放）。
     */
    @Override
    public void draw(Graphics g, int x, int y, int w, int h) {
        synchronized (lock) {
            if (g == null || w <= 0 || h <= 0) return;
            Image img = getImage();
            if (img != null) g.drawImage(img, x, y, w, h, null);
        }
    }

    /**
     * 计算当前已播放的毫秒数（暂停时不增加）。
     */
    private long elapsedMs() {
        long now = System.currentTimeMillis();
        return playing ? (now - playStartMs) : pausedElapsedMs;
    }

    /**
     * 根据时间与帧率计算当前序号，单线程调用下无锁。
     */
    private int currentFrameIndexUnlocked() {
        int count = getFrameCount();
        if (count <= 1) return 0;
        double fps = Math.max(1.0, getFrameRate());
        long el = elapsedMs();
        long frameDur = (long) Math.max(1.0, 1000.0 / fps);
        return (int) ((el / frameDur) % count);
    }

    // 路径/格式辅助方法
    private boolean isDirectorySequencePath(String p) {
        if (p == null) return false;
        String s = p.trim();
        return s.endsWith("/") || s.endsWith("/*");
    }

    private String normalizeDirPath(String p) {
        String d = (p == null) ? "/" : p.trim();
        if (d.endsWith("/*")) d = d.substring(0, d.length() - 2);
        if (!d.endsWith("/")) d += "/";
        if (!d.startsWith("/")) d = "/" + d;
        return d;
    }

    private boolean isImageExt(String name) {
        String s = name.toLowerCase(Locale.ROOT);
        return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg");
    }

    /**
     * 帧名比较：优先按数字部分排序，否则字典序。
     */
    private int compareNames(String a, String b) {
        String an = lastName(a);
        String bn = lastName(b);
        int na = numericPart(an);
        int nb = numericPart(bn);
        if (na >= 0 && nb >= 0) return Integer.compare(na, nb);
        return an.compareTo(bn);
    }

    private String lastName(String s) {
        if (s == null) return "";
        int idx = s.lastIndexOf('/');
        return idx >= 0 ? s.substring(idx + 1) : s;
    }

    private int numericPart(String name) {
        int start = -1, end = -1;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isDigit(c)) { if (start < 0) start = i; end = i; }
            else if (start >= 0) break;
        }
        if (start >= 0 && end >= start) {
            try { return Integer.parseInt(name.substring(start, end + 1)); }
            catch (Exception ignore) {}
        }
        return -1;
    }
}
