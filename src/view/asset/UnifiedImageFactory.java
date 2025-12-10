/*
 UnifiedImageFactory
 职责:
   - 基于路径后缀选择 AnimatedImage 或 StaticImage 实现
   - 支持扩展：registerExtension(".webp", factory) 可在不修改源码前提下扩展格式
 规范:
   - 类路径资源统一补全前导 /
   - URL/本地绝对路径原样保留
*/
package view.asset;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 统一图片资源工厂
 * 根据文件类型自动创建合适的图片对象
 */
public class UnifiedImageFactory {
    // 简易扩展点：后缀 -> (key,path)->ImageInterface
    private static final Map<String, BiFunction<String, String, ImageInterface>> EXT_FACTORIES = new ConcurrentHashMap<>();

    /**
     * 注册自定义扩展（例如 ".webp"），传入后缀与工厂函数。
     *
     * @param extension 后缀（包含点，如 ".webp"）
     * @param factory   (key,path)->ImageInterface 创建逻辑
     */
    public static void registerExtension(String extension, BiFunction<String, String, ImageInterface> factory) {
        if (extension == null || factory == null) return;
        EXT_FACTORIES.put(extension.toLowerCase(Locale.ROOT), factory);
    }

    /**
     * 根据路径推断类型并创建图片对象。
     *
     * @param key  资源键
     * @param path 原始路径（类路径/绝对路径）
     * @return 已创建的图片对象
     * @throws IllegalArgumentException 不支持格式时抛出
     */
    public static ImageInterface createImage(String key, String path) {
        if (path == null || path.trim().isEmpty()) throw new IllegalArgumentException("图片路径不能为空");
        // 新增：规范类路径资源，确保使用“/绝对”形式（忽略 URL/本地绝对路径）
        String normalized = normalizeClasspathPath(path);
        String lower = normalized.toLowerCase(Locale.ROOT);

        // 扩展优先
        for (Map.Entry<String, BiFunction<String, String, ImageInterface>> e : EXT_FACTORIES.entrySet()) {
            if (lower.endsWith(e.getKey())) {
                return e.getValue().apply(key, normalized);
            }
        }

        if (lower.endsWith(".gif")) return new AnimatedImage(key, normalized);
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".bmp"))
            return new StaticImage(key, normalized);
        throw new IllegalArgumentException("不支持的图片格式: " + normalized);
    }

    /**
     * 显式指定是否强制动画类型。
     */
    public static ImageInterface createImage(String key, String path, boolean forceAnimated) {
        return forceAnimated ? new AnimatedImage(key, path) : new StaticImage(key, path);
    }

    /**
     * 类路径规范化：补全前导 /；若是 URL/盘符/UNC 直接返回。
     */
    private static String normalizeClasspathPath(String path) {
        String p = path.trim();
        // URL 或 file: / Windows 盘符 / UNC 路径直接返回
        if (p.matches("^[a-zA-Z]+:.*") || p.matches("^[a-zA-Z]:[\\\\/].*") || p.startsWith("\\\\")) return p;
        return p.startsWith("/") ? p : "/" + p;
    }
}