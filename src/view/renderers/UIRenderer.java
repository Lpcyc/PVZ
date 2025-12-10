package view.renderers;

import model.GameLogicUpdater;
import model.entities.PlantCard;
import view.asset.AssetKey;
import view.asset.AssetLoader;
import view.asset.ImageInterface;

import java.awt.*;
import java.util.List;

public class UIRenderer {
    private static final int CHOOSER_X = 120;
    private static final int CHOOSER_Y = 5;
    private static final int CHOOSER_WIDTH = 522;
    private static final int CHOOSER_HEIGHT = 95; // slightly shorter to avoid covering top row
    private static final int CARD_AREA_PADDING_X = 85;
    private static final int CARD_AREA_PADDING_Y = 5; // moved slightly up to avoid covering plants
    private static final int CARD_WIDTH = 60;
    private static final int CARD_HEIGHT = 82;
    private static final int CARD_SPACING = 70;
    private static final int SUN_TEXT_OFFSET_X = 25;
    private static final int SUN_TEXT_OFFSET_Y = 78; // lowered more as requested
    private static final int SUN_BG_WIDTH = 60;
    private static final int SUN_BG_HEIGHT = 28;
    private static final int SUN_BG_OFFSET_X = -10;
    private static final int SUN_BG_OFFSET_Y = -24;

    private final AssetLoader assetLoader;
    private final GameLogicUpdater gameLogic;
    private final Font sunFont;

    public UIRenderer(AssetLoader assetLoader, GameLogicUpdater gameLogic) {
        this.assetLoader = assetLoader;
        this.gameLogic = gameLogic;
        this.sunFont = new Font("Arial", Font.BOLD, 26);
    }

    public void render(Graphics g, PlantCard selectedCard, boolean plantingMode) {
        if (!(g instanceof Graphics2D)) { return; }
        Graphics2D g2 = (Graphics2D) g;
        Object aa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawChooserBackground(g2);
        drawSunValue(g2);
        drawPlantCards(g2, selectedCard, plantingMode);
        drawShovel(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
    }

    private void drawChooserBackground(Graphics2D g2) {
        ImageInterface chooser = assetLoader.getImage(AssetKey.UI_CHOOSER_BG.getId());
        if (chooser != null && chooser.isLoaded()) {
            g2.drawImage(chooser.getImage(), CHOOSER_X, CHOOSER_Y, CHOOSER_WIDTH, CHOOSER_HEIGHT, null);
        }
    }

    private void drawSunValue(Graphics2D g2) {
        int textX = CHOOSER_X + SUN_TEXT_OFFSET_X;
        int textY = CHOOSER_Y + SUN_TEXT_OFFSET_Y;
        int bgX = textX + SUN_BG_OFFSET_X;
        int bgY = textY + SUN_BG_OFFSET_Y;
        g2.setColor(new Color(255, 247, 180, 220));
        g2.fillRoundRect(bgX, bgY, SUN_BG_WIDTH, SUN_BG_HEIGHT, 12, 12);
        g2.setColor(new Color(255, 204, 0));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bgX, bgY, SUN_BG_WIDTH, SUN_BG_HEIGHT, 12, 12);
        g2.setColor(Color.BLACK);
        g2.setFont(sunFont);
        g2.drawString(String.valueOf(gameLogic.getSun()), textX, textY);
    }

    private void drawPlantCards(Graphics2D g2, PlantCard selectedCard, boolean plantingMode) {
        int cardX = CHOOSER_X + CARD_AREA_PADDING_X;
        int cardY = CHOOSER_Y + CARD_AREA_PADDING_Y;
        List<PlantCard> cards = gameLogic.getPlantCards();
        for (PlantCard card : cards) {
            Rectangle bounds = new Rectangle(cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
            ImageInterface cardImage = assetLoader.getImage(card.getCardImageKey().getId());
            if (cardImage != null && cardImage.isLoaded()) {
                g2.drawImage(cardImage.getImage(), cardX, cardY, CARD_WIDTH, CARD_HEIGHT, null);
            } else {
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRect(cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
            }

            if (selectedCard != null && selectedCard == card && plantingMode) {
                g2.setColor(new Color(255, 230, 120, 140)); // warmer yellow overlay
                g2.fill(bounds);
                g2.setColor(new Color(255, 200, 0));        // stronger yellow border
                g2.setStroke(new BasicStroke(3f));
                g2.draw(bounds);
            }

            cardX += CARD_SPACING;
        }
    }

    private void drawShovel(Graphics2D g2) {
        int slotX = CHOOSER_X + CHOOSER_WIDTH - 80;
        int slotY = CHOOSER_Y + 12;
        ImageInterface slot = assetLoader.getImage(AssetKey.UI_SHOVEL_SLOT.getId());
        if (slot != null && slot.isLoaded()) {
            g2.drawImage(slot.getImage(), slotX, slotY, 70, 70, null);
        }
        ImageInterface shovel = assetLoader.getImage(AssetKey.UI_SHOVEL.getId());
        if (shovel != null && shovel.isLoaded()) {
            g2.drawImage(shovel.getImage(), slotX + 5, slotY + 5, 50, 50, null);
        }
    }
}