package view.asset;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AssetKey
 * 职责: 统一注册并描述游戏资源的键（id/path/type/category），提供集中式缓存入口。
 * 设计要点:
 *  - REGISTRY 保存 Key 元数据（不可变），不直接存图像对象。
 *  - CACHE 使用 SoftReference 允许 JVM 在内存紧张时回收图片。
 * 使用规范:
 *  - 通过 AssetLoader 实际加载与管理生命周期。
 *  - 通过 get(loader, key) 获取并自动加载后缓存。
 */
public final class AssetKey {
    public enum AssetType { STATIC, ANIMATED }
    public enum Category { BACKGROUND, PLANT, ZOMBIE, UI, EFFECT, MISC }

    /** 类型安全资源键（不可变） */
    public static final class Key<T extends ImageInterface> {
        private final String id;
        private final String path;
        private final AssetType type;
        private final Category category;
        private Key(String id, String path, AssetType type, Category category) {
            this.id = Objects.requireNonNull(id);
            this.path = Objects.requireNonNull(path);
            this.type = Objects.requireNonNull(type);
            this.category = Objects.requireNonNull(category);
        }
        public String getId() { return id; }
        public String getPath() { return path; }
        public AssetType getType() { return type; }
        public Category getCategory() { return category; }
        @Override public String toString() { return id + " -> " + path; }
        @Override public boolean equals(Object o){ return (o instanceof Key)&&id.equals(((Key<?>)o).id); }
        @Override public int hashCode(){ return id.hashCode(); }
    }

    private static final Map<String, Key<?>> REGISTRY = new LinkedHashMap<>();
    private static final Map<String, SoftReference<ImageInterface>> CACHE = new ConcurrentHashMap<>();

    private AssetKey() {}

    // 内置键
    public static final Key<ImageInterface> MAIN_LAWN =
            register("main_lawn", "/backgrounds/main_lawn.jpg", AssetType.STATIC, Category.BACKGROUND);
    public static final Key<ImageInterface> PLANT_SUNFLOWER =
            register("plant_sunflower", "/plants/sunflower/sunflower.gif", AssetType.ANIMATED, Category.PLANT);
    public static final Key<ImageInterface> PLANT_PEASHOOTER =
            register("plant_peashooter", "/plants/peashooter/idle/peashooter.gif", AssetType.ANIMATED, Category.PLANT);
    public static final Key<ImageInterface> ZOMBIE_NORMAL =
            register("zombie_basic", "/zombies/normalZombies/walk/", AssetType.ANIMATED, Category.ZOMBIE);
    public static final Key<ImageInterface> BULLET_PEA =
            register("bullet_pea", "/bullet/PeaNormal.png", AssetType.STATIC, Category.EFFECT);

    // UI and Game Elements from external project
    public static final Key<ImageInterface> UI_TOOLBAR =
            register("ui_toolbar", "/ui/bar5.png", AssetType.STATIC, Category.UI);
    public static final Key<ImageInterface> UI_SHOVEL =
            register("ui_shovel", "/ui/shovel.png", AssetType.STATIC, Category.UI);
    public static final Key<ImageInterface> UI_SHOVEL_SLOT =
            register("ui_shovel_slot", "/ui/shovelSlot.png", AssetType.STATIC, Category.UI);
    public static final Key<ImageInterface> UI_LAWNMOWER =
            register("ui_lawnmower", "/ui/car.png", AssetType.STATIC, Category.UI);
    public static final Key<ImageInterface> UI_CHOOSER_BG =
            register("ui_chooser_bg", "/ui/ChooserBackground.png", AssetType.STATIC, Category.UI);

    // Plant Cards
    public static final Key<ImageInterface> CARD_PEASHOOTER =
            register("card_peashooter", "/ui/cards/Peashoot.png", AssetType.STATIC, Category.UI);
    public static final Key<ImageInterface> CARD_SUNFLOWER =
            register("card_sunflower", "/ui/cards/Sunflower.png", AssetType.STATIC, Category.UI);
    public static final Key<ImageInterface> CARD_CHERRYBOMB =
            register("card_cherrybomb", "/ui/cards/card_cherrybomb.png", AssetType.STATIC, Category.UI);
    public static final Key<ImageInterface> CARD_WALLNUT =
            register("card_wallnut", "/ui/cards/card_wallnut.png", AssetType.STATIC, Category.UI);

    public static final Key<ImageInterface> PLANT_PEASHOOTER_IDLE_SEQ =
            registerSeq("plant_peashooter_idle", "/plants/peashooter/idle/peashooter.seq", Category.PLANT);
    public static final Key<ImageInterface> PLANT_SUNFLOWER_IDLE_SEQ =
            registerSeq("plant_sunflower_idle", "/plants/sunflower/idle/sunflower.seq", Category.PLANT);
    public static final Key<ImageInterface> ZOMBIE_BASIC_WALK_SEQ =
            registerSeq("zombie_basic_walk_seq", "/zombies/normal/walk/normalZombie.seq", Category.ZOMBIE);

    public static final Key<ImageInterface> PLANT_PEASHOOTER_IDLE_DIR =
            registerSeq("plant_peashooter_idle_dir", "/plants/peashooter/idle/", Category.PLANT);
    public static final Key<ImageInterface> PLANT_SUNFLOWER_IDLE_DIR =
            registerSeq("plant_sunflower_idle_dir", "/plants/sunflower/idle/", Category.PLANT);
    public static final Key<ImageInterface> ZOMBIE_NORMAL_WALK_DIR =
            registerSeq("zombie_normal_walk_dir", "/zombies/normalZombies/walk/", Category.ZOMBIE);

    /**
     * 注册资源键（若重复注册不同路径会抛出异常）。
     * @param id 逻辑唯一标识
     * @param path 类路径（前导 /）
     * @param type STATIC 或 ANIMATED
     * @param category 分类（背景/植物/僵尸等）
     */
    public static synchronized <T extends ImageInterface> Key<T> register(String id, String path, AssetType type, Category category) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("资源 id 不能为空");
        Key<?> old = REGISTRY.get(id);
        if (old != null) {
            // 如果完全重复，直接返回；否则禁止覆盖（避免误替换）
            if (old.getPath().equals(path) && old.getType() == type && old.getCategory() == category) {
                @SuppressWarnings("unchecked") Key<T> cast = (Key<T>) old;
                return cast;
            }
            throw new IllegalStateException("资源键重复注册: " + id + " 原路径=" + old.getPath() + " 新路径=" + path);
        }
        Key<T> key = new Key<>(id, path, type, category);
        REGISTRY.put(id, key);
        return key;
    }

    public static Key<ImageInterface> registerSeq(String id, String path, Category category) {
        return register(id, path, AssetType.ANIMATED, category);
    }

    /** 根据 id 获取已注册的 Key（可能为 null）。 */
    public static Key<?> of(String id) { return REGISTRY.get(id); }

    /** 只读快照：所有键的 (id -> path) 映射，便于调试或外部遍历。 */
    public static Map<String,String> snapshotIdToPath() {
        Map<String,String> m = new LinkedHashMap<>();
        for (Key<?> k : REGISTRY.values()) m.put(k.getId(), k.getPath());
        return Collections.unmodifiableMap(m);
    }

    public static Collection<Key<?>> allKeys() { return Collections.unmodifiableCollection(REGISTRY.values()); }

    public static List<Key<?>> byCategory(Category category) {
        List<Key<?>> list = new ArrayList<>();
        for (Key<?> k : REGISTRY.values()) if (k.getCategory() == category) list.add(k);
        return list;
    }

    public static List<Key<?>> defaultPreloadKeys() {
        return Arrays.asList(MAIN_LAWN, PLANT_SUNFLOWER, PLANT_PEASHOOTER, ZOMBIE_NORMAL, BULLET_PEA);
    }

    public static List<Key<?>> sequenceKeys() {
        return Arrays.asList(
                PLANT_PEASHOOTER_IDLE_DIR,
                ZOMBIE_NORMAL_WALK_DIR
        );
    }

    public static ImageInterface get(AssetLoader loader, Key<?> key) {
        if (key == null || loader == null) return null;
        ImageInterface hit = deref(CACHE.get(key.getId()));
        if (hit != null && hit.isLoaded()) return hit;
        ImageInterface img = loader.getImageSafe(key.getId());
        if (img == null || !img.isLoaded()) img = loader.loadImage(key.getId(), key.getPath());
        if (img != null) CACHE.put(key.getId(), new SoftReference<>(img));
        return img;
    }

    public static void invalidate(Key<?> key) {
        if (key != null) CACHE.remove(key.getId());
    }

    public static void invalidateAll() { CACHE.clear(); }

    private static ImageInterface deref(SoftReference<ImageInterface> ref) {
        return ref == null ? null : ref.get();
    }
}
