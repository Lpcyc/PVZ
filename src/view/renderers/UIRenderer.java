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
    private static final int SUN_BG_WIDTH = 80;
    private static final int SUN_BG_HEIGHT = 35;
    private static final int SUN_BG_OFFSET_X = -15;
    private static final int SUN_BG_OFFSET_Y = -27;
    private static final int COOLDOWN_DURATION = 7500; // 7.5 seconds cooldown

    private final AssetLoader assetLoader;
    private final GameLogicUpdater gameLogic;
    private final Font sunFont;
    private final Font costFont;
    private final Font tooltipFont;
    private Point mousePos;

    public UIRenderer(AssetLoader assetLoader, GameLogicUpdater gameLogic) {
        this.assetLoader = assetLoader;
        this.gameLogic = gameLogic;
        this.sunFont = new Font("Arial", Font.BOLD, 28);
        this.costFont = new Font("Arial", Font.BOLD, 14);
        this.tooltipFont = new Font("Arial", Font.PLAIN, 12);
        this.mousePos = new Point(-1, -1);
    }

    public void setMousePosition(Point pos) {
        this.mousePos = pos;
    }
    
    public Point getMousePosition() {
        return this.mousePos;
    }

    public void render(Graphics g, PlantCard selectedCard, boolean plantingMode) {
        if (!(g instanceof Graphics2D)) { return; }
        Graphics2D g2 = (Graphics2D) g;
        Object aa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
        
        // Enhanced sun counter background with gradient
        GradientPaint gradient = new GradientPaint(
            bgX, bgY, new Color(255, 250, 200, 240),
            bgX, bgY + SUN_BG_HEIGHT, new Color(255, 235, 150, 240)
        );
        g2.setPaint(gradient);
        g2.fillRoundRect(bgX, bgY, SUN_BG_WIDTH, SUN_BG_HEIGHT, 15, 15);
        
        // Outer border
        g2.setColor(new Color(200, 150, 0));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(bgX, bgY, SUN_BG_WIDTH, SUN_BG_HEIGHT, 15, 15);
        
        // Inner highlight
        g2.setColor(new Color(255, 255, 200, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bgX + 2, bgY + 2, SUN_BG_WIDTH - 4, SUN_BG_HEIGHT - 4, 12, 12);
        
        // Sun icon (simplified)
        int iconX = bgX + 5;
        int iconY = bgY + SUN_BG_HEIGHT / 2;
        drawSunIcon(g2, iconX, iconY, 12);
        
        // Sun value text with shadow
        g2.setFont(sunFont);
        String sunText = String.valueOf(gameLogic.getSun());
        
        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(sunText, textX + 1, textY + 1);
        
        // Main text
        g2.setColor(new Color(139, 69, 19)); // Brown color for better contrast
        g2.drawString(sunText, textX, textY);
    }
    
    private void drawSunIcon(Graphics2D g2, int cx, int cy, int radius) {
        // Draw sun rays
        g2.setColor(new Color(255, 200, 0));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int x1 = cx + (int)(radius * 0.6 * Math.cos(angle));
            int y1 = cy + (int)(radius * 0.6 * Math.sin(angle));
            int x2 = cx + (int)(radius * 1.2 * Math.cos(angle));
            int y2 = cy + (int)(radius * 1.2 * Math.sin(angle));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x1, y1, x2, y2);
        }
        
        // Draw sun center
        GradientPaint sunGradient = new GradientPaint(
            cx - radius/2, cy - radius/2, new Color(255, 255, 100),
            cx + radius/2, cy + radius/2, new Color(255, 200, 0)
        );
        g2.setPaint(sunGradient);
        g2.fillOval(cx - radius/2, cy - radius/2, radius, radius);
        
        g2.setColor(new Color(255, 220, 0));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - radius/2, cy - radius/2, radius, radius);
    }

    private void drawPlantCards(Graphics2D g2, PlantCard selectedCard, boolean plantingMode) {
        int cardX = CHOOSER_X + CARD_AREA_PADDING_X;
        int cardY = CHOOSER_Y + CARD_AREA_PADDING_Y;
        List<PlantCard> cards = gameLogic.getPlantCards();
        long currentTime = System.currentTimeMillis();
        
        for (PlantCard card : cards) {
            Rectangle bounds = new Rectangle(cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
            boolean canAfford = gameLogic.getSun() >= card.getCost();
            boolean isOnCooldown = isCardOnCooldown(card, currentTime);
            boolean isHovered = bounds.contains(mousePos);
            
            // Draw card background with state
            drawCardBackground(g2, bounds, canAfford, isOnCooldown);
            
            // Draw card image
            ImageInterface cardImage = assetLoader.getImage(card.getCardImageKey().getId());
            if (cardImage != null && cardImage.isLoaded()) {
                g2.drawImage(cardImage.getImage(), cardX, cardY, CARD_WIDTH, CARD_HEIGHT, null);
            } else {
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRect(cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
            }

            // Draw cooldown overlay
            if (isOnCooldown) {
                drawCooldownOverlay(g2, bounds, card, currentTime);
            }
            
            // Draw "not enough sun" overlay
            if (!canAfford && !isOnCooldown) {
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fill(bounds);
                g2.setColor(new Color(80, 80, 80));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(bounds);
            }

            // Draw selection highlight
            if (selectedCard != null && selectedCard == card && plantingMode) {
                g2.setColor(new Color(255, 230, 120, 160)); // warmer yellow overlay
                g2.fill(bounds);
                g2.setColor(new Color(255, 200, 0));        // stronger yellow border
                g2.setStroke(new BasicStroke(4f));
                g2.draw(bounds);
            }
            
            // Draw hover effect
            if (isHovered && canAfford && !isOnCooldown) {
                g2.setColor(new Color(255, 255, 255, 80));
                g2.fill(bounds);
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(bounds);
            }
            
            // Draw cost badge
            drawCostBadge(g2, cardX, cardY, card.getCost(), canAfford);
            
            // Draw tooltip on hover
            if (isHovered) {
                drawCardTooltip(g2, cardX, cardY, card);
            }

            cardX += CARD_SPACING;
        }
    }
    
    private void drawCardBackground(Graphics2D g2, Rectangle bounds, boolean canAfford, boolean isOnCooldown) {
        if (isOnCooldown) {
            g2.setColor(new Color(60, 60, 60));
        } else if (canAfford) {
            g2.setColor(new Color(240, 240, 240));
        } else {
            g2.setColor(new Color(150, 150, 150));
        }
        g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        // Border
        g2.setColor(new Color(100, 80, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
    
    private void drawCooldownOverlay(Graphics2D g2, Rectangle bounds, PlantCard card, long currentTime) {
        long elapsed = currentTime - card.getLastUsedTime();
        float progress = Math.min(1.0f, (float)elapsed / COOLDOWN_DURATION);
        
        // Dark overlay
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fill(bounds);
        
        // Progress bar from bottom to top
        int overlayHeight = (int)(bounds.height * (1 - progress));
        g2.setColor(new Color(100, 100, 100, 180));
        g2.fillRect(bounds.x, bounds.y, bounds.width, overlayHeight);
        
        // Progress text
        int remainingSeconds = (int)Math.ceil((COOLDOWN_DURATION - elapsed) / 1000.0);
        if (remainingSeconds > 0) {
            g2.setColor(Color.WHITE);
            g2.setFont(costFont);
            String timeText = String.valueOf(remainingSeconds);
            FontMetrics fm = g2.getFontMetrics();
            int textX = bounds.x + (bounds.width - fm.stringWidth(timeText)) / 2;
            int textY = bounds.y + bounds.height / 2 + fm.getAscent() / 2;
            
            // Shadow
            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawString(timeText, textX + 1, textY + 1);
            
            // Text
            g2.setColor(Color.WHITE);
            g2.drawString(timeText, textX, textY);
        }
    }
    
    private void drawCostBadge(Graphics2D g2, int cardX, int cardY, int cost, boolean canAfford) {
        int badgeX = cardX + 2;
        int badgeY = cardY + CARD_HEIGHT - 20;
        int badgeW = 30;
        int badgeH = 18;
        
        // Badge background
        Color badgeColor = canAfford ? new Color(255, 235, 150, 220) : new Color(200, 100, 100, 220);
        g2.setColor(badgeColor);
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
        
        // Badge border
        g2.setColor(canAfford ? new Color(200, 150, 0) : new Color(150, 50, 50));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
        
        // Cost text
        g2.setFont(costFont);
        String costText = String.valueOf(cost);
        FontMetrics fm = g2.getFontMetrics();
        int textX = badgeX + (badgeW - fm.stringWidth(costText)) / 2;
        int textY = badgeY + (badgeH + fm.getAscent()) / 2 - 1;
        
        g2.setColor(new Color(60, 30, 0));
        g2.drawString(costText, textX, textY);
    }
    
    private void drawCardTooltip(Graphics2D g2, int cardX, int cardY, PlantCard card) {
        String tooltip = card.getName() + " - Cost: " + card.getCost();
        g2.setFont(tooltipFont);
        FontMetrics fm = g2.getFontMetrics();
        int tooltipW = fm.stringWidth(tooltip) + 10;
        int tooltipH = fm.getHeight() + 6;
        int tooltipX = cardX + (CARD_WIDTH - tooltipW) / 2;
        int tooltipY = cardY - tooltipH - 5;
        
        // Ensure tooltip stays on screen
        if (tooltipY < 0) {
            tooltipY = cardY + CARD_HEIGHT + 5;
        }
        
        // Tooltip background
        g2.setColor(new Color(50, 50, 50, 230));
        g2.fillRoundRect(tooltipX, tooltipY, tooltipW, tooltipH, 8, 8);
        
        // Tooltip border
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(tooltipX, tooltipY, tooltipW, tooltipH, 8, 8);
        
        // Tooltip text
        g2.setColor(Color.WHITE);
        g2.drawString(tooltip, tooltipX + 5, tooltipY + fm.getAscent() + 3);
    }
    
    private boolean isCardOnCooldown(PlantCard card, long currentTime) {
        if (card.getLastUsedTime() < 0) return false;
        return (currentTime - card.getLastUsedTime()) < COOLDOWN_DURATION;
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