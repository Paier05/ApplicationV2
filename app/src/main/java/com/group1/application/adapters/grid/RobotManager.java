package com.group1.application.adapters.grid;

/**
 * Manages robot state and operations in the grid.
 * The robot is 2x2. The 'center' coordinates now refer to the TOP-LEFT cell of the robot.
 */
public class RobotManager {
    // Position of the top-left cell of the robot
    private int centerRow = -1;
    private int centerCol = -1;
    private int orientation = GridConstants.ORIENTATION_NORTH;
    private int tempCenterRow = -1; // Represents top-left of temporary robot
    private int tempCenterCol = -1;

    private static final int ROBOT_SIZE = 2;

    /**
     * Check if robot is placed on the grid
     */
    public boolean hasRobot() {
        return centerRow != -1 && centerCol != -1;
    }

    /**
     * Get robot's top-left position
     */
    public int[] getRobotPosition() {
        if (!hasRobot()) return null;
        return new int[]{centerRow, centerCol};
    }

    /**
     * Check if a 2x2 robot can be placed with its top-left corner at the given position.
     * The 'center' parameters now refer to the TOP-LEFT cell.
     */
    public boolean canPlaceAtCenter(int topLeftRow, int topLeftCol, GridCell[] cells) {
        // Check grid boundaries for a 2x2 robot
        if (topLeftRow < 0 || (topLeftRow + ROBOT_SIZE - 1) >= GridConstants.DATA_SIZE) return true; // Invalid row
        if (topLeftCol < 1 || (topLeftCol + ROBOT_SIZE - 1) >= GridConstants.GRID_SIZE) return true; // Invalid col

        // Check collisions with permanent obstacles in the 2x2 area
        for (int r = topLeftRow; r < topLeftRow + ROBOT_SIZE; r++) {
            for (int c = topLeftCol; c < topLeftCol + ROBOT_SIZE; c++) {
                int pos = r * GridConstants.GRID_SIZE + c;
                GridCell cell = cells[pos];
                // Block if there is a permanent obstacle that is not part of the current robot
                if (cell.isObstacle() && !cell.isRobot()) return true;
            }
        }
        return false; // Can be placed
    }

    /**
     * Place robot with its top-left corner at the given position.
     * The 'center' parameters now refer to the TOP-LEFT cell.
     */
    public boolean placeAtCenter(int topLeftRow, int topLeftCol, GridCell[] cells) {
        if (canPlaceAtCenter(topLeftRow, topLeftCol, cells)) return false;

        // Clear existing robot
        clearRobot(cells);

        // Mark new 2x2 area as robot cells
        for (int r = topLeftRow; r < topLeftRow + ROBOT_SIZE; r++) {
            for (int c = topLeftCol; c < topLeftCol + ROBOT_SIZE; c++) {
                int pos = r * GridConstants.GRID_SIZE + c;
                GridCell cell = cells[pos];
                cell.setRobot(true);
                cell.setObstacle(true); // Robot is an obstacle
                cell.setTemporaryObstacle(false);
                cell.setData("R");
                cell.setColor(GridConstants.ROBOT_COLOR);
                cell.setBorderDirection(null);
                cell.setBorderColor(0);
            }
        }

        this.centerRow = topLeftRow;
        this.centerCol = topLeftCol;
        applyFrontMarker(cells);
        return true;
    }

    /**
     * Clear the 2x2 robot from grid
     */
    public void clearRobot(GridCell[] cells) {
        if (!hasRobot()) return;

        for (int r = centerRow; r < centerRow + ROBOT_SIZE; r++) {
            for (int c = centerCol; c < centerCol + ROBOT_SIZE; c++) {
                // Bounds check
                if (r >= 0 && r < GridConstants.DATA_SIZE && c > 0 && c < GridConstants.GRID_SIZE) {
                    int pos = r * GridConstants.GRID_SIZE + c;
                    GridCell cell = cells[pos];
                    if (cell.isRobot()) {
                        cell.setRobot(false);
                        cell.setObstacle(false);
                        cell.setTemporaryObstacle(false);
                        cell.setData("");
                        cell.setColor(GridConstants.TRAVERSED_PATH_COLOR);
                        cell.setBorderDirection(null);
                        cell.setBorderColor(0);
                    }
                }
            }
        }
        centerRow = -1;
        centerCol = -1;
    }

    /**
     * Turn robot left
     */
    public void turnLeft(GridCell[] cells) {
        if (!hasRobot()) return;
        int oldTopLeftRow = this.centerRow;
        int oldTopLeftCol = this.centerCol;
        orientation = (orientation + 3) % 4; // -1 mod 4
        // Re-place the robot at the same location to update its visual orientation
        placeAtCenter(oldTopLeftRow, oldTopLeftCol, cells);
    }

    /**
     * Turn robot right
     */
    public void turnRight(GridCell[] cells) {
        if (!hasRobot()) return;
        int oldTopLeftRow = this.centerRow;
        int oldTopLeftCol = this.centerCol;
        orientation = (orientation + 1) % 4;
        // Re-place the robot at the same location to update its visual orientation
        placeAtCenter(oldTopLeftRow, oldTopLeftCol, cells);
    }


    /**
     * Move robot by delta
     */
    public boolean moveRobot(int dRow, int dCol, GridCell[] cells) {
        if (!hasRobot()) return false;
        int newTopLeftRow = centerRow + dRow;
        int newTopLeftCol = centerCol + dCol;

        // Store current robot position and orientation
        int oldTopLeftRow = centerRow;
        int oldTopLeftCol = centerCol;
        int oldOrientation = orientation;

        // Temporarily clear the robot to check for collisions properly
        clearRobot(cells);

        // Check if the new position is valid
        if (canPlaceAtCenter(newTopLeftRow, newTopLeftCol, cells)) {
            // If move failed, restore the robot at its original position
            this.orientation = oldOrientation; // restore orientation
            placeAtCenter(oldTopLeftRow, oldTopLeftCol, cells);
            return false;
        }

        // If move is successful, place the robot at the new position
        this.orientation = oldOrientation; // Keep same orientation during move
        return placeAtCenter(newTopLeftRow, newTopLeftCol, cells);
    }

    /**
     * Apply front marker color based on orientation. Marks two cells as the head.
     */
    private void applyFrontMarker(GridCell[] cells) {
        if (!hasRobot()) return;

        // First, set all robot cells to the default robot color
        for (int r = centerRow; r < centerRow + ROBOT_SIZE; r++) {
            for (int c = centerCol; c < centerCol + ROBOT_SIZE; c++) {
                int pos = r * GridConstants.GRID_SIZE + c;
                cells[pos].setColor(GridConstants.ROBOT_COLOR);
            }
        }

        // Determine the two head cells based on orientation and color them
        int r1 = -1, c1 = -1, r2 = -1, c2 = -1;

        switch (orientation) {
            case GridConstants.ORIENTATION_NORTH:
                r1 = centerRow; c1 = centerCol;
                r2 = centerRow; c2 = centerCol + 1;
                break;
            case GridConstants.ORIENTATION_EAST:
                r1 = centerRow; c1 = centerCol + 1;
                r2 = centerRow + 1; c2 = centerCol + 1;
                break;
            case GridConstants.ORIENTATION_SOUTH:
                r1 = centerRow + 1; c1 = centerCol;
                r2 = centerRow + 1; c2 = centerCol + 1;
                break;
            case GridConstants.ORIENTATION_WEST:
                r1 = centerRow; c1 = centerCol;
                r2 = centerRow + 1; c2 = centerCol;
                break;
        }

        if (r1 != -1) { // If a valid orientation was found
            int headPos1 = r1 * GridConstants.GRID_SIZE + c1;
            int headPos2 = r2 * GridConstants.GRID_SIZE + c2;
            cells[headPos1].setColor(GridConstants.ROBOT_FRONT_COLOR);
            cells[headPos2].setColor(GridConstants.ROBOT_FRONT_COLOR);
        }
    }

    /**
     * Update robot position from external coordinates.
     * The incoming x, y are treated as the desired top-left corner of the robot.
     */
    public boolean updatePosition(int x, int y, String direction, GridCell[] cells) {
        // Convert display coordinates (0-19, origin at bottom-left) to grid coordinates (top-left of robot)
        int gridCol = x + 1;
        int gridRow = (GridConstants.DATA_SIZE - 1) - y;

        int newOrientation = parseDirection(direction);
        if (newOrientation == -1) return false;

        // Temporarily set orientation to check placement correctly
        int oldOrientation = this.orientation;
        this.orientation = newOrientation;

        if (placeAtCenter(gridRow, gridCol, cells)) {
            // Placement successful, orientation is already set
            return true;
        } else {
            // Revert orientation if placement failed
            this.orientation = oldOrientation;
            return false;
        }
    }

    /**
     * Parse direction string to orientation
     */
    private int parseDirection(String direction) {
        if (direction == null) return -1;
        switch (direction.toUpperCase().trim()) {
            case "N": return GridConstants.ORIENTATION_NORTH;
            case "E": return GridConstants.ORIENTATION_EAST;
            case "S": return GridConstants.ORIENTATION_SOUTH;
            case "W": return GridConstants.ORIENTATION_WEST;
            default: return -1;
        }
    }

    public int getOrientation() { return orientation; }
    public int getTempCenterRow() { return tempCenterRow; }

    // Temporary robot preview methods
    /**
     * Shows a 2x2 temporary robot preview.
     * 'center' params now mean top-left.
     */
    public boolean showTemporaryRobotAtCenter(int topLeftRow, int topLeftCol, GridCell[] cells) {
        // Use a modified canPlaceAtCenter that ignores temporary robots for previews
        if (canPlaceAtCenter(topLeftRow, topLeftCol, cells)) return false;
        clearTemporaryRobotPreview(cells);

        for (int r = topLeftRow; r < topLeftRow + ROBOT_SIZE; r++) {
            for (int c = topLeftCol; c < topLeftCol + ROBOT_SIZE; c++) {
                 int pos = r * GridConstants.GRID_SIZE + c;
                GridCell cell = cells[pos];
                if (!cell.isObstacle() && !cell.isRobot()) {
                    cell.setColor(GridConstants.TEMP_ROBOT_COLOR);
                }
                cell.setTempRobot(true);
            }
        }
        tempCenterRow = topLeftRow;
        tempCenterCol = topLeftCol;
        return true;
    }

    public void clearTemporaryRobotPreview(GridCell[] cells) {
        if (tempCenterRow == -1 || tempCenterCol == -1) {
            // Fallback for safety
            for (int i = 0; i < GridConstants.TOTAL_CELLS; i++) {
                GridCell cell = cells[i];
                if (cell.isTempRobot()) {
                    int row = i / GridConstants.GRID_SIZE;
                    int col = i % GridConstants.GRID_SIZE;
                    if (col > 0 && row < GridConstants.DATA_SIZE && !cell.isObstacle() && !cell.isRobot()) {
                        cell.setColor(GridConstants.DEFAULT_CELL_COLOR);
                    }
                    cell.setTempRobot(false);
                }
            }
            tempCenterRow = -1;
            tempCenterCol = -1;
            return;
        }

        for (int r = tempCenterRow; r < tempCenterRow + ROBOT_SIZE; r++) {
            for (int c = tempCenterCol; c < tempCenterCol + ROBOT_SIZE; c++) {
                 if (r >= 0 && r < GridConstants.DATA_SIZE && c > 0 && c < GridConstants.GRID_SIZE) {
                    int pos = r * GridConstants.GRID_SIZE + c;
                    GridCell cell = cells[pos];
                    if (cell.isTempRobot() && !cell.isObstacle() && !cell.isRobot()) {
                        cell.setColor(GridConstants.DEFAULT_CELL_COLOR);
                    }
                    cell.setTempRobot(false);
                }
            }
        }
        tempCenterRow = -1;
        tempCenterCol = -1;
    }

    public boolean confirmTemporaryRobotPlacement(GridCell[] cells) {
        if (tempCenterRow == -1 || tempCenterCol == -1) return false;
        int r = tempCenterRow, c = tempCenterCol;
        clearTemporaryRobotPreview(cells);
        return placeAtCenter(r, c, cells);
    }
}
