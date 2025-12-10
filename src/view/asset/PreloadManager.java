/*
 PreloadManager
 职责: 在启动阶段同步（或未来扩展为异步）加载核心资源，返回失败列表用于日志或重试。
*/
package view.asset;
import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class PreloadManager {
    private static final PreloadManager INSTANCE = new PreloadManager();
    private PreloadManager() {}
    public static PreloadManager getInstance() { return INSTANCE; }

    /**
     * Asynchronously preloads the default set of assets and calls a completion handler on the EDT.
     * @param onComplete A consumer that accepts a boolean indicating success.
     */
    public static void preloadAssets(Consumer<Boolean> onComplete) {
        new Thread(() -> {
            AssetLoader loader = AssetLoader.getInstance();
            List<String> failed = INSTANCE.preloadInitial(loader);
            boolean success = failed.isEmpty();
            if (!success) {
                System.err.println("Failed to preload the following assets: " + failed);
            }
            SwingUtilities.invokeLater(() -> onComplete.accept(success));
        }, "Asset-Preloader-Thread").start();
    }

    /**
     * 预加载默认资源集合（背景+常用实体），返回加载失败的键列表。
     */
    public List<String> preloadInitial(AssetLoader loader) {
        List<String> failed = new ArrayList<>();
        if (loader == null) return failed;
        try { if (loader.loadImage(AssetKey.MAIN_LAWN) == null) failed.add(AssetKey.MAIN_LAWN.getId()); } catch (Exception ignore) {}
        List<AssetKey.Key<?>> keys = AssetKey.defaultPreloadKeys();
        for (AssetKey.Key<?> k : keys) {
            try { if (loader.loadImage(k) == null) failed.add(k.getId()); } catch (Exception e) { failed.add(k.getId()); }
        }
        return failed;
    }

    /**
     * 按指定键集合预加载（忽略未注册的键），返回失败列表。
     */
    public List<String> preloadKeys(AssetLoader loader, Collection<String> keys) {
        List<String> failed = new ArrayList<>();
        if (loader == null || keys == null) return failed;
        for (String id : keys) {
            AssetKey.Key<?> k = AssetKey.of(id);
            if (k != null) {
                try { if (loader.loadImage(k) == null) failed.add(id); } catch (Exception e) { failed.add(id); }
            }
        }
        return failed;
    }
}
