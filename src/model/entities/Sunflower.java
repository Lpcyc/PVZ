package model.entities;

//向日葵类
public class Sunflower extends Plant implements Entity.AnimatedSprite, Entity.GridAligned {
    private static final int BASE_INTERVAL_TICKS = 150;   // 15 seconds @10 FPS
    private static final int MIN_INTERVAL_TICKS = 60;     // 6 seconds floor
    private static volatile double globalIntervalMultiplier = 1.0;

    private int sunProductionTimer;
    private int dynamicIntervalTicks = BASE_INTERVAL_TICKS;
    private int sunValue = 25;
    private boolean readyToCollect = false;
    private boolean hasCustomInterval = false;
    private double localMultiplier = globalIntervalMultiplier;

    public Sunflower(int row, int column) {
        super(row, column, 50, 80);
        this.sunProductionTimer = 0;
        refreshDynamicInterval(); // Initialize interval based on global multiplier
    }

    public Sunflower(int x, int y, int row, int column) {
        super(x, y, row, column, 50, 80);
        this.sunProductionTimer = 0;
        refreshDynamicInterval(); // Initialize interval based on global multiplier
    }

    @Override
    public void update() {
        super.update();
        if (!alive) {
            readyToCollect = false;
            sunProductionTimer = 0;
            return;
        }
        syncGlobalMultiplierIfNeeded();
        if (!readyToCollect) {
            sunProductionTimer++;
            if (sunProductionTimer >= dynamicIntervalTicks) {
                sunProductionTimer = 0;
                readyToCollect = true;
            }
        }
    }

    private void syncGlobalMultiplierIfNeeded() {
        if (hasCustomInterval) return;
        double target = globalIntervalMultiplier;
        if (Math.abs(localMultiplier - target) > 1e-4) {
            localMultiplier = target;
            refreshDynamicInterval();
        }
    }

    private void refreshDynamicInterval() {
        int target = (int) Math.max(MIN_INTERVAL_TICKS, Math.round(BASE_INTERVAL_TICKS * localMultiplier));
        if (dynamicIntervalTicks != target) {
            dynamicIntervalTicks = target;
        }
    }

    // ===== 生产/收集接口（保持不变）=====
    /** 是否已经产出阳光（等待收集） */
    public boolean shouldProduceSun() {
        return readyToCollect && alive;
    }

    /**
     * Called by the system when a player clicks on this sunflower.
     * If sun is ready, it returns the sun value and resets the production state.
     * @return The amount of sun collected, or 0 if not ready.
     */
    public int collectSun() {
        if (!readyToCollect) return 0;
        readyToCollect = false;
        sunProductionTimer = 0;
        if (!hasCustomInterval) {
            refreshDynamicInterval();
        }
        return sunValue;
    }

    /** A legacy method, now just calls collectSun(). Kept for compatibility if needed. */
    public int tryCollectSun() {
        // This method is no longer intended for automatic collection.
        // It can be removed if no other parts of the system rely on it.
        return 0;
    }

    // 可调参数（便于关卡平衡）
    public int getProductionInterval() { return dynamicIntervalTicks; }
    public void setProductionInterval(int productionInterval) {
        this.dynamicIntervalTicks = Math.max(1, productionInterval);
        this.hasCustomInterval = true;
    }
    public int getSunValue() { return sunValue; }
    public void setSunValue(int sunValue) {
        this.sunValue = Math.max(0, sunValue);
    }
    public int getSunProductionTimer() { return sunProductionTimer; }


    // ===== AnimatedSprite 资源键 =====
    @Override
    public String getSpriteKey() {
        return "plant_sunflower";
    }

    // ===== GridAligned 网格接口 =====
    @Override
    public int getGridRow() { return this.row; }

    @Override
    public int getGridCol() { return this.column; }

    @Override
    public void setGridPosition(int row, int col) {
        this.row = row;
        this.column = col;
        // 若需要：同步像素坐标到格子中心/底部，可在此加入转换逻辑
    }

    public static void applyGlobalProductionBoost(int sunflowerCount) {
        double targetMultiplier = Math.max(0.25, 1.0 - sunflowerCount * 0.15);
        if (Math.abs(targetMultiplier - globalIntervalMultiplier) > 1e-4) {
            globalIntervalMultiplier = targetMultiplier;
        }
    }
}