package model.entities;

import view.asset.AssetKey;

/**
 * Represents a plant card in the game's UI.
 * Each card has a plant type, a sun cost, and an associated image.
 */
public class PlantCard {
    private final String plantType;
    private final int sunCost;
    private final AssetKey.Key<?> cardImageKey;
    private long lastUsedTime; // For cooldown tracking

    public PlantCard(String plantType, int sunCost, AssetKey.Key<?> cardImageKey) {
        this.plantType = plantType;
        this.sunCost = sunCost;
        this.cardImageKey = cardImageKey;
        this.lastUsedTime = -1; // -1 means never used
    }

    public String getPlantType() {
        return plantType;
    }

    public String getName() {
        return plantType;
    }

    public int getSunCost() {
        return sunCost;
    }

    // Legacy alias for compatibility with older references
    public int getCost() {
        return sunCost;
    }

    public AssetKey.Key<?> getCardImageKey() {
        return cardImageKey;
    }

    public long getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(long lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }
}
