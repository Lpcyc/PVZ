package model.entities;

import java.awt.Rectangle;
import java.util.List;

/**
 * LawnMower Entity.
 * It stays at the beginning of a lane and activates when a zombie reaches it.
 * When activated, it moves forward, killing all zombies in its path.
 */
public class LawnMower extends Entity {
    private final int row;
    private boolean activated;
    private static final float SPEED = 4.0f;

    public LawnMower(int x, int y, int row) {
        super(x, y, 70, 70, 9999); // High health, effectively invulnerable
        this.row = row;
        this.activated = false;
    }

    @Override
    public void update() {
        if (activated && alive) {
            move(SPEED, 0);
            // Deactivate when it goes off-screen
            if (this.x > 1000) { // Assuming screen width is around 1000
                this.alive = false;
            }
        }
    }

    public void activate() {
        this.activated = true;
    }

    public boolean isActivated() {
        return activated;
    }

    public int getRow() {
        return row;
    }

    /**
     * Checks for collisions with zombies and kills them.
     * @param zombies The list of zombies to check against.
     */
    public void checkCollisions(List<Zombie> zombies) {
        if (!activated || !alive) {
            return;
        }
        Rectangle mowerBounds = getBounds();
        for (Zombie zombie : zombies) {
            if (zombie.isAlive() && zombie.getRow() == this.row) {
                if (mowerBounds.intersects(zombie.getBounds())) {
                    zombie.takeDamage(9999); // Instantly kill the zombie
                }
            }
        }
    }
}

