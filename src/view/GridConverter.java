package view;

/**
 * GridConverter - Utility class to convert grid coordinates to pixel coordinates.
 * This centralizes the grid logic, making it easy to adjust the layout.
 */
public class GridConverter {

    // These values define the grid's layout on the screen.
    // They might need to be adjusted to match the background image perfectly.
    public static final int GRID_START_X = 250; // The x-pixel where the first column starts.
    public static final int GRID_START_Y = 80;  // The y-pixel where the first row starts.
    public static final int CELL_WIDTH = 80;    // The width of a single grid cell.
    public static final int CELL_HEIGHT = 100;  // The height of a single grid cell.

    /**
     * Converts a grid column index to a pixel x-coordinate.
     * @param col The column index (starting from 0).
     * @return The corresponding x-coordinate in pixels.
     */
    public static int colToX(int col) {
        return GRID_START_X + col * CELL_WIDTH;
    }

    /**
     * Converts a grid row index to a pixel y-coordinate.
     * @param row The row index (starting from 0).
     * @return The corresponding y-coordinate in pixels.
     */
    public static int rowToY(int row) {
        return GRID_START_Y + row * CELL_HEIGHT;
    }
}

