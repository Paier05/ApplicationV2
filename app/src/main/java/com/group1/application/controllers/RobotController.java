package com.group1.application.controllers;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.group1.application.adapters.GridTableAdapter;
import com.group1.application.bluetooth.BluetoothHelper;

/**
 * Controller for all robot-related UI and logic.
 * Manages placing the 2x2 robot, movement, and status updates.
 */
public class RobotController {

    private final Context context;
    private GridTableAdapter gridAdapter;
    private BluetoothHelper bluetoothHelper;

    // UI components
    private Button placeRobotButton;
    private Button robotConfirmButton;
    private Button robotUpButton, robotDownButton, robotTurnLeftButton, robotTurnRightButton;
    private TextView robotStatusText;
    private EditText robotCenterXInput, robotCenterYInput;
    private Button placeRobotByCoordButton;
    private TextView robotPositionStatusText;

    // State
    private boolean isPlacingRobotMode = false;
    private boolean suppressCancelToastOnce = false;

    public RobotController(Context context) {
        this.context = context;
    }

    public void initialize(GridTableAdapter gridAdapter, BluetoothHelper bluetoothHelper) {
        this.gridAdapter = gridAdapter;
        this.bluetoothHelper = bluetoothHelper;
    }

    public void setUIComponents(Button placeRobotButton, Button robotConfirmButton,
                               Button robotUpButton, Button robotDownButton,
                               Button robotTurnLeftButton, Button robotTurnRightButton,
                               TextView robotStatusText, EditText robotCenterXInput,
                               EditText robotCenterYInput, Button placeRobotByCoordButton, TextView robotPositionStatusText) {
        this.placeRobotButton = placeRobotButton;
        this.robotConfirmButton = robotConfirmButton;
        this.robotUpButton = robotUpButton;
        this.robotDownButton = robotDownButton;
        this.robotTurnLeftButton = robotTurnLeftButton;
        this.robotTurnRightButton = robotTurnRightButton;
        this.robotStatusText = robotStatusText;
        this.robotCenterXInput = robotCenterXInput;
        this.robotCenterYInput = robotCenterYInput;
        this.placeRobotByCoordButton = placeRobotByCoordButton;
        this.robotPositionStatusText = robotPositionStatusText;

        setupClickListeners();
    }

    private void setupClickListeners() {
        if (placeRobotButton != null) {
            placeRobotButton.setOnClickListener(v -> togglePlaceRobotMode());
        }
        if (robotConfirmButton != null) {
            robotConfirmButton.setOnClickListener(v -> confirmRobotPlacement());
        }
        if (robotUpButton != null) {
            robotUpButton.setOnClickListener(v -> moveForward());
        }
        if (robotDownButton != null) {
            robotDownButton.setOnClickListener(v -> moveReverse());
        }
        if (robotTurnLeftButton != null) {
            robotTurnLeftButton.setOnClickListener(v -> turnRobotLeft());
        }
        if (robotTurnRightButton != null) {
            robotTurnRightButton.setOnClickListener(v -> turnRobotRight());
        }
        if (placeRobotByCoordButton != null) {
            placeRobotByCoordButton.setOnClickListener(v -> placeRobotByCoordinates());
        }
    }

    // Robot Actions
    public void moveForward() {
        if (gridAdapter != null && gridAdapter.hasRobot()) {
            int dRow = 0, dCol = 0;
            switch (gridAdapter.getRobotOrientation()) {
                case 0: dRow = -1; break; // North
                case 1: dCol = 1;  break; // East
                case 2: dRow = 1;  break; // South
                case 3: dCol = -1; break; // West
            }
            boolean moved = gridAdapter.moveRobot(dRow, dCol);
            if (moved) {
                updateRobotStatusText();
                sendBluetoothCommand("f");
            } else {
                showToast("Blocked - cannot move forward");
            }
        } else {
            showToast("Place the robot first");
        }
    }

    public void moveReverse() {
        if (gridAdapter != null && gridAdapter.hasRobot()) {
            int dRow = 0, dCol = 0;
            switch (gridAdapter.getRobotOrientation()) {
                case 0: dRow = 1; break;   // opposite of North
                case 1: dCol = -1; break;  // opposite of East
                case 2: dRow = -1; break;  // opposite of South
                case 3: dCol = 1; break;   // opposite of West
            }
            boolean moved = gridAdapter.moveRobot(dRow, dCol);
            if (moved) {
                updateRobotStatusText();
                sendBluetoothCommand("r");
            } else {
                showToast("Blocked - cannot move reverse");
            }
        } else {
            showToast("Place the robot first");
        }
    }

    public void turnRobotLeft() {
        if (gridAdapter != null && gridAdapter.hasRobot()) {
            gridAdapter.turnRobotLeft();
            updateRobotStatusText();
            sendBluetoothCommand("tl");
        } else {
            showToast("Place the robot first");
        }
    }

    public void turnRobotRight() {
        if (gridAdapter != null && gridAdapter.hasRobot()) {
            gridAdapter.turnRobotRight();
            updateRobotStatusText();
            sendBluetoothCommand("tr");
        } else {
            showToast("Place the robot first");
        }
    }

    // Placement Mode Logic
    public void togglePlaceRobotMode() {
        isPlacingRobotMode = !isPlacingRobotMode;
        setPlacingRobotMode(isPlacingRobotMode);
    }

    public void setPlacingRobotMode(boolean enabled) {
        isPlacingRobotMode = enabled;
        if (enabled) {
            beginContinuousDragIfSupported();
            if (gridAdapter != null) gridAdapter.clearTemporaryRobotPreview();
            if (placeRobotButton != null) {
                placeRobotButton.setText("Cancel Placement");
            }
            showToast("Drag on grid to preview the robot, then tap Confirm");
        } else {
            endContinuousDragIfActive();
            if (gridAdapter != null) gridAdapter.clearTemporaryRobotPreview();
            if (placeRobotButton != null) {
                placeRobotButton.setText("Place Robot (2x2)");
            }
            if (!suppressCancelToastOnce) {
                showToast("Robot placement cancelled");
            }
            suppressCancelToastOnce = false; // reset flag
        }
        updateConfirmButtonVisibility();
    }

    public boolean handleRobotPlacementClick(int row, int col) {
        if (!isPlacingRobotMode || gridAdapter == null) return false;
        return previewRobotAt(row, col);
    }

    public boolean previewRobotAt(int row, int col) {
        if (!isPlacingRobotMode || gridAdapter == null) return false;
        beginContinuousDragIfSupported();
        boolean shown = gridAdapter.showTemporaryRobotAtCenter(row, col);
        updateConfirmButtonVisibility();
        if (robotStatusText != null && shown) {
            int displayRow = gridAdapter.getGridSize() - 2 - row;
            int displayCol = col - 1;
            robotStatusText.setText("Preview at top-left: (" + displayRow + ", " + displayCol + ")");
        }
        return true;
    }

    public void confirmRobotPlacement() {
        if (!isPlacingRobotMode || gridAdapter == null) return;
        boolean placed = gridAdapter.confirmTemporaryRobotPlacement();
        if (placed) {
            int[] pos = gridAdapter.getRobotPosition();
            if (pos != null && robotStatusText != null) {
                int displayRow = gridAdapter.getGridSize() - 2 - pos[0];
                int displayCol = pos[1] - 1;
                robotStatusText.setText("Robot placed at top-left: (" + displayRow + ", " + displayCol + ")");
            }
            suppressCancelToastOnce = true;
            endContinuousDragIfActive();
            setPlacingRobotMode(false);
            updateRobotButtonsState();
        } else {
            showToast("Invalid position - cannot place robot");
        }
        updateConfirmButtonVisibility();
    }

    private void updateConfirmButtonVisibility() {
        if (robotConfirmButton != null) {
            boolean show = isPlacingRobotMode && gridAdapter != null && gridAdapter.getTempRobotCenterRow() != -1;
            robotConfirmButton.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
            robotConfirmButton.setEnabled(show);
        }
    }

    public void updateRobotStatusText() {
        if (robotStatusText != null && gridAdapter != null) {
            if (gridAdapter.hasRobot()) {
                int[] robotPos = gridAdapter.getRobotPosition(); // This is top-left
                if (robotPos != null && robotPos.length >= 2) {
                    int displayRow = gridAdapter.getGridSize() - 2 - robotPos[0];
                    int displayCol = robotPos[1] - 1;
                    String orientation = getRobotOrientationString(gridAdapter.getRobotOrientation());
                    robotStatusText.setText("Robot at (" + displayRow + ", " + displayCol + ") facing " + orientation);
                } else {
                    robotStatusText.setText("Robot position unknown");
                }
            } else {
                robotStatusText.setText("Robot not placed");
            }
        }
    }

    public void updateRobotButtonsState() {
        if (placeRobotButton != null && !isPlacingRobotMode) {
            placeRobotButton.setText("Place Robot (2x2)");
        }
        boolean hasRobot = gridAdapter != null && gridAdapter.hasRobot();
        if (robotUpButton != null) robotUpButton.setEnabled(hasRobot);
        if (robotDownButton != null) robotDownButton.setEnabled(hasRobot);
        if (robotTurnLeftButton != null) robotTurnLeftButton.setEnabled(hasRobot);
        if (robotTurnRightButton != null) robotTurnRightButton.setEnabled(hasRobot);
    }

    private String getRobotOrientationString(int orientation) {
        switch (orientation) {
            case 0: return "North";
            case 1: return "East";
            case 2: return "South";
            case 3: return "West";
            default: return "Unknown";
        }
    }

    private void sendBluetoothCommand(String command) {
        if (bluetoothHelper != null && bluetoothHelper.isConnected()) {
            bluetoothHelper.sendData(command);
        } else {
            showToast("Not connected");
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public boolean isPlacingRobotMode() {
        return isPlacingRobotMode;
    }

    public void exitPlacementMode() {
        if (isPlacingRobotMode) {
            endContinuousDragIfActive();
            setPlacingRobotMode(false);
        }
    }

    /**
     * Place a 2x2 robot at the specified top-left coordinates from user input.
     */
    private void placeRobotByCoordinates() {
        if (robotCenterXInput == null || robotCenterYInput == null || robotPositionStatusText == null) {
            showToast("Input fields not available");
            return;
        }

        String xText = robotCenterXInput.getText().toString().trim();
        String yText = robotCenterYInput.getText().toString().trim();

        if (xText.isEmpty() || yText.isEmpty()) {
            updateRobotPositionStatus("Please enter both X and Y coordinates", false);
            return;
        }

        try {
            int displayX = Integer.parseInt(xText);
            int displayY = Integer.parseInt(yText);

            // Validate coordinate ranges for a 2x2 robot's top-left corner (0-18)
            if (displayX < 0 || displayX > 18) {
                updateRobotPositionStatus("X coordinate must be between 0 and 18 for a 2x2 robot", false);
                return;
            }
            if (displayY < 0 || displayY > 18) {
                updateRobotPositionStatus("Y coordinate must be between 0 and 18 for a 2x2 robot", false);
                return;
            }

            int gridCol = displayX + 1;
            int gridRow = (gridAdapter.getGridSize() - 2) - displayY;

            boolean placed = gridAdapter.updateRobotPosition(displayX, displayY, "N");

            if (placed) {
                robotCenterXInput.setText("");
                robotCenterYInput.setText("");
                updateRobotStatusText();
                updateRobotButtonsState();
                updateRobotPositionStatus("Robot placed at top-left (" + displayX + ", " + displayY + ") facing North", true);
                showToast("Robot placed at (" + displayX + ", " + displayY + ")");

                if (isPlacingRobotMode) {
                    suppressCancelToastOnce = true;
                    setPlacingRobotMode(false);
                }
            } else {
                updateRobotPositionStatus("Cannot place robot at (" + displayX + ", " + displayY + ") - space occupied or too close to edge", false);
            }
        } catch (NumberFormatException e) {
            updateRobotPositionStatus("Please enter valid numbers for coordinates", false);
        }
    }

    /**
     * Update the status text for robot position input
     */
    private void updateRobotPositionStatus(String message, boolean isSuccess) {
        if (robotPositionStatusText != null) {
            robotPositionStatusText.setText(message);
            robotPositionStatusText.setTextColor(isSuccess ? 0xFF4CAF50 : 0xFFF44336);
        }
    }

    // Drag support
    private void beginContinuousDragIfSupported() {
        if (gridAdapter != null && !gridAdapter.isInContinuousDrag()) {
            gridAdapter.beginContinuousDrag();
        }
    }

    private void endContinuousDragIfActive() {
        if (gridAdapter != null && gridAdapter.isInContinuousDrag()) {
            gridAdapter.endContinuousDrag();
        }
    }
}
