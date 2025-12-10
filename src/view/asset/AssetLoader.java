/*
 AssetLoader
 职责: 单例集中管理资源加载与缓存（静态 + 序列动画）
 增强:
   - loadImage(AssetKey.Key) 重载支持
   - 异步加载入口 loadImageAsync
   - 解析/推断时的空值防御与并发安全
*/
package view.asset;

import java.awt.Image;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AssetLoader {
    private static AssetLoader instance;
    private final Map<String, ImageInterface> imageCache;
    private final Set<String> loadingInProgress;
    private final Map<String ,String> resourcePathMap;
    private final ExecutorService executor;

    private AssetLoader() {
        this.imageCache = new ConcurrentHashMap<>();
        this.loadingInProgress = Collections.synchronizedSet(new HashSet<>());
        this.resourcePathMap = new HashMap<>();
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ResourceLoader-Background");
                t.setDaemon(true);
                return t;
            }
        });
        for (Map.Entry<String, String> e : AssetKey.snapshotIdToPath().entrySet()) {
            resourcePathMap.put(e.getKey(), e.getValue());
        }
    }

    public static synchronized AssetLoader getInstance() {
        if (instance == null) instance = new AssetLoader();
        return instance;
    }

    /**
     * 主入口（显式路径版）：若已缓存直接返回，否则创建并加载。
     * 并发：同一 key 若正在加载，返回已有缓存（可能未完全加载）。
     */
    public ImageInterface loadImage(String key, String path) {
        if (key == null || path == null) return null;
        key = normalizeKey(key);
        ImageInterface cached = imageCache.get(key);
        if (cached != null && cached.isLoaded()) return cached;
        if (loadingInProgress.contains(key)) return imageCache.get(key);
        try {
            loadingInProgress.add(key);
            String resolvedPath = normalizeExplicitPath(key, path);
            ImageInterface image = isSequencePath(resolvedPath)
                    ? new AnimatedImage(key, resolvedPath)
                    : UnifiedImageFactory.createImage(key, resolvedPath);
            boolean ok = false;
            try { ok = image.load(); } catch (Throwable ignore) {}
            if (ok) {
                imageCache.put(key, image);
                return image;
            }
            return null;
        } finally {
            loadingInProgress.remove(key);
        }
    }

    /**
     * 按 key 推断路径加载（常用字符串简化调用）。
     */
    public ImageInterface loadImage(String key){
        if (key == null) return null;
        key = normalizeKey(key);
        ImageInterface cached = imageCache.get(key);
        if (cached != null && cached.isLoaded()) return cached;
        String configured = resourcePathMap.get(key);
        String resolved = configured != null ? resolveResourcePath(key, configured) : null;
        if (resolved == null) resolved = tryHeuristic(key);
        if (resolved == null) return null;
        return loadImage(key, resolved);
    }

    /**
     * 类型安全重载：直接使用 AssetKey.Key 进行加载。
     */
    public ImageInterface loadImage(AssetKey.Key<?> key) {
        if (key == null) return null;
        return loadImage(key.getId(), key.getPath());
    }

    /**
     * 异步加载：在后台线程执行并回调结果（可能为 null）。
     */
    public void loadImageAsync(String key, String path, java.util.function.Consumer<ImageInterface> cb) {
        if (key == null || path == null) return;
        executor.submit(() -> {
            ImageInterface img = loadImage(key, path);
            if (cb != null) {
                try { cb.accept(img); } catch (Exception ignore) {}
            }
        });
    }

    private String tryHeuristic(String key) {
        String suffix = key.contains("_") ? key.substring(key.indexOf('_') + 1) : key;
        if (suffix == null || suffix.isEmpty()) return null;
        String[] dirs = key.startsWith("zombie_") ? new String[] {"zombies",""} :
                        key.startsWith("plant_")  ? new String[] {"plants",""} :
                        new String[] {"images","assets",""};
        String[] exts = {"gif","png","jpg","jpeg"};
        for (String d : dirs) {
            for (String ext : exts) {
                String cand = (d.isEmpty()? "/" + suffix : "/" + d + "/" + suffix) + "." + ext;
                if (existsOnClassPath(cand)) return cand;
            }
        }
        if (key.startsWith("zombie")) {
            String base = resourcePathMap.getOrDefault("zombie_basic", "/zombies/normalZombies/walk/");
            return resolveResourcePath("zombie_basic", base);
        }
        if (key.startsWith("plant")) {
            String base = resourcePathMap.getOrDefault("plant_peashooter", "/plants/peashooter/idle/peashooter.gif");
            return resolveResourcePath("plant_peashooter", base);
        }
        return null;
    }

    public Map<String, ImageInterface> loadImages(Map<String, String> imageMap) {
        if (imageMap == null) return Collections.emptyMap();
        Map<String, ImageInterface> result = new HashMap<>();
        for (Map.Entry<String, String> entry : imageMap.entrySet()) {
            ImageInterface image = loadImage(entry.getKey(), entry.getValue());
            result.put(entry.getKey(), image);
        }
        return result;
    }

    /**
     * 获取已加载或尝试加载（自动推断路径）后的资源。
     * @return 已加载对象或 null
     */
    public ImageInterface getImage(String key) {
        if (key == null) return null;
        key = normalizeKey(key);
        ImageInterface image = imageCache.get(key);
        return (image != null) ? image : loadImage(key);
    }

    /**
     * 安全获取（不触发加载）缓存对象。
     */
    public ImageInterface getImageSafe(String key) {
        if (key == null) return null;
        return imageCache.get(normalizeKey(key));
    }

    /**
     * 获取 AWT Image（若未加载则异步尝试加载并返回 null）。
     */
    public Image getAwImage(String key){
        if (key == null) return null;
        key = normalizeKey(key);
        ImageInterface ii = imageCache.get(key);
        if (ii != null && ii.isLoaded()) return ii.getImage();
        if (!loadingInProgress.contains(key)) {
            String fk = key;
            executor.submit(() -> { try { loadImage(fk); } catch (Exception ignore) {} });
        }
        return null;
    }
    public Image getAwtImage(String key) { return getAwImage(key); }
    public Image getAwtImage(AssetKey.Key<?> key) { return key == null ? null : getAwImage(key.getId()); }

    /**
     * 是否已经加载。
     */
    public boolean isImageLoaded(String key) {
        if (key == null) return false;
        ImageInterface image = imageCache.get(normalizeKey(key));
        return image != null && image.isLoaded();
    }

    /**
     * 卸载单个资源（释放内存并从缓存移除）。
     */
    public void unloadImage(String key) {
        if (key == null) return;
        key = normalizeKey(key);
        ImageInterface image = imageCache.remove(key);
        if (image != null) image.unload();
    }

    /**
     * 卸载全部资源并关闭后台线程池。
     */
    public void unloadAll() {
        for (ImageInterface image : imageCache.values()) {
            try { image.unload(); } catch (Exception ignore) {}
        }
        imageCache.clear();
        loadingInProgress.clear();
        try { executor.shutdownNow(); } catch (Exception ignore) {}
    }

    // --- 内部路径/存在性工具与别名规范化 ---
    private String resolveResourcePath(String key, String primary) {
        if (primary != null) {
            String p = primary.trim();
            if (isSequencePath(p)) {
                String abs = p.startsWith("/") ? p : "/" + p;
                if (abs.endsWith("/*") || abs.endsWith(".seq") || abs.endsWith("/")) return abs;
            }
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(primary);
        if (primary != null) candidates.add(trimLeadingSlash(primary));
        for (String c : candidates) {
            if (c == null || c.isEmpty()) continue;
            String abs = c.startsWith("/") ? c : "/" + c;
            String rel = c.startsWith("/") ? c.substring(1) : c;
            if (existsOnClassPath(abs)) return abs;
            if (existsOnClassPath(rel)) return "/" + rel;
        }
        return null;
    }

    private boolean existsOnClassPath(String path) {
        if (path == null) return false;
        URL u1 = AssetLoader.class.getResource(path.startsWith("/")? path : "/" + path);
        if (u1 != null) return true;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL u2 = (cl == null) ? null : cl.getResource(path.startsWith("/")? path.substring(1) : path);
        return u2 != null;
    }

    private String trimLeadingSlash(String p) {
        if (p == null) return null;
        return p.startsWith("/") ? p.substring(1) : p;
    }

    private String normalizeExplicitPath(String key, String path) {
        if (path == null || path.trim().isEmpty()) return path;
        String p = path.trim();
        if (looksLikeFirstPngFrame(p)) {
            String noTrail = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
            int idx = noTrail.lastIndexOf('/');
            if (idx > 0) {
                String dir = noTrail.substring(0, idx + 1);
                return dir.startsWith("/") ? dir : "/" + dir;
            }
        }
        if (isSequencePath(p)) {
            String abs = p.startsWith("/") ? p : "/" + p;
            if (abs.endsWith("/*") || abs.endsWith(".seq")) return abs;
            return abs.endsWith("/") ? abs : (abs + "/");
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(p);
        candidates.add(p.startsWith("/") ? p.substring(1) : "/" + p);
        for (String c : candidates) {
            String abs = c.startsWith("/") ? c : "/" + c;
            String rel = c.startsWith("/") ? c.substring(1) : c;
            if (existsOnClassPath(abs) || existsOnClassPath(rel)) return abs;
        }
        return p.startsWith("/") ? p : "/" + p;
    }

    private boolean isSequencePath(String p) {
        if (p == null) return false;
        String s = p.trim().toLowerCase(Locale.ROOT);
        return s.endsWith(".seq") || s.endsWith("/") || s.endsWith("/*");
    }

    private boolean looksLikeFirstPngFrame(String p) {
        if (p == null) return false;
        String s = p.trim();
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        s = s.toLowerCase(Locale.ROOT);
        return s.endsWith("/1.png");
    }

    public void pauseAllAnimations() {
        imageCache.values().forEach(img -> {
            if (img != null && img.isAnimated()) try { img.pause(); } catch (Exception ignore) {}
        });
    }

    public void resumeAllAnimations() {
        imageCache.values().forEach(img -> {
            if (img != null && img.isAnimated()) try { img.play(); } catch (Exception ignore) {}
        });
    }

    private String normalizeKey(String key) {
        if (key == null) return null;
        String k = key.trim();
        if (k.equals("zombie") || k.equals("zombies")) return "zombie_basic";
        return k;
    }
}
