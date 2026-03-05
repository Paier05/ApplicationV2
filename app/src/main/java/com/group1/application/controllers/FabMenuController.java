package com.group1.application.controllers;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Controller class to handle FAB menu operations
 */
public class FabMenuController {

    private final android.content.Context context;

    // FAB menu components
    private FloatingActionButton fabMain, fabSend, fabReceive, fabObstacle, fabRobot, fabImage;
    private View fabSendLabel, fabReceiveLabel, fabObstacleLabel, fabRobotLabel, fabImageLabel;
    private ScrollView sendDataSection, receiveDataSection, obstacleControlSection, robotControlSection, imageSection;
    private LinearLayout gridTableSection;
    private View fabScrim;
    private boolean isFabMenuOpen = false;

    private OnFabMenuListener listener;

    public interface OnFabMenuListener {
        void onSendDataSectionSelected();
        void onReceiveDataSectionSelected();
        void onObstacleModeToggled();
        void onRobotPanelToggled();
        void onImageSectionSelected();
    }

    public FabMenuController(android.content.Context context) {
        this.context = context;
    }

    public void initialize(OnFabMenuListener listener) {
        this.listener = listener;
    }

    public void setUIComponents(FloatingActionButton fabMain, FloatingActionButton fabSend,
                               FloatingActionButton fabReceive, FloatingActionButton fabObstacle,
                               FloatingActionButton fabRobot,
                               FloatingActionButton fabImage,
                               View fabSendLabel, View fabReceiveLabel, View fabObstacleLabel,
                               View fabRobotLabel, View fabImageLabel,
                               ScrollView sendDataSection, ScrollView receiveDataSection,
                               ScrollView obstacleControlSection, ScrollView robotControlSection,
                               ScrollView imageSection,
                               LinearLayout gridTableSection,
                               View fabScrim) {
        this.fabMain = fabMain;
        this.fabSend = fabSend;
        this.fabReceive = fabReceive;
        this.fabObstacle = fabObstacle;
        this.fabRobot = fabRobot;
        this.fabImage = fabImage;
        this.fabSendLabel = fabSendLabel;
        this.fabReceiveLabel = fabReceiveLabel;
        this.fabObstacleLabel = fabObstacleLabel;
        this.fabRobotLabel = fabRobotLabel;
        this.fabImageLabel = fabImageLabel;
        this.sendDataSection = sendDataSection;
        this.receiveDataSection = receiveDataSection;
        this.obstacleControlSection = obstacleControlSection;
        this.robotControlSection = robotControlSection;
        this.imageSection = imageSection;
        this.gridTableSection = gridTableSection;
        this.fabScrim = fabScrim;

        setupClickListeners();
    }

    private void setupClickListeners() {
        if (fabMain != null) {
            fabMain.setOnClickListener(v -> toggleFabMenu());
        }

        if (fabScrim != null) {
            fabScrim.setOnClickListener(v -> closeFabMenu());
        }

        if (fabSend != null) {
            fabSend.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSendDataSectionSelected();
                }
                closeFabMenu();
            });
        }

        if (fabReceive != null) {
            fabReceive.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReceiveDataSectionSelected();
                }
                closeFabMenu();
            });
        }

        if (fabObstacle != null) {
            fabObstacle.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onObstacleModeToggled();
                }
                closeFabMenu();
            });
        }

        if (fabRobot != null) {
            fabRobot.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRobotPanelToggled();
                }
                closeFabMenu();
            });
        }

        if (fabImage != null) {
            fabImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageSectionSelected();
                }
                closeFabMenu();
            });
        }
    }

    public void toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu();
        } else {
            openFabMenu();
        }
    }

    public void openFabMenu() {
        isFabMenuOpen = true;

        // Show sub FABs and labels with animation
        setFabVisibility();
        animateFabsIn();

        // Rotate main FAB
        if (fabMain != null) {
            fabMain.animate().rotation(45f).setDuration(300);
        }

        // Show scrim
        if (fabScrim != null) {
            fabScrim.setVisibility(View.VISIBLE);
            fabScrim.animate().alpha(1.0f).setDuration(300);
        }
    }

    public void closeFabMenu() {
        isFabMenuOpen = false;

        // Hide sub FABs and labels with animation
        animateFabsOut();

        // Rotate main FAB back
        if (fabMain != null) {
            fabMain.animate().rotation(0f).setDuration(300);
        }

        // Hide scrim
        if (fabScrim != null) {
            fabScrim.animate().alpha(0.0f).setDuration(300).withEndAction(() -> fabScrim.setVisibility(View.GONE));
        }
    }

    private void setFabVisibility() {
        if (fabSend != null) fabSend.setVisibility(View.VISIBLE);
        if (fabReceive != null) fabReceive.setVisibility(View.VISIBLE);
        if (fabObstacle != null) fabObstacle.setVisibility(View.VISIBLE);
        if (fabRobot != null) fabRobot.setVisibility(View.VISIBLE);
        if (fabImage != null) fabImage.setVisibility(View.VISIBLE);
        if (fabSendLabel != null) fabSendLabel.setVisibility(View.VISIBLE);
        if (fabReceiveLabel != null) fabReceiveLabel.setVisibility(View.VISIBLE);
        if (fabObstacleLabel != null) fabObstacleLabel.setVisibility(View.VISIBLE);
        if (fabRobotLabel != null) fabRobotLabel.setVisibility(View.VISIBLE);
        if (fabImageLabel != null) fabImageLabel.setVisibility(View.VISIBLE);
    }

    private void animateFabsIn() {
        if (fabSend != null) {
            fabSend.animate().translationY(0).alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabReceive != null) {
            fabReceive.animate().translationY(0).alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabObstacle != null) {
            fabObstacle.animate().translationY(0).alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabRobot != null) {
            fabRobot.animate().translationY(0).alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabImage != null) {
            fabImage.animate().translationY(0).alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabSendLabel != null) {
            fabSendLabel.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabReceiveLabel != null) {
            fabReceiveLabel.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabObstacleLabel != null) {
            fabObstacleLabel.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabRobotLabel != null) {
            fabRobotLabel.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
        if (fabImageLabel != null) {
            fabImageLabel.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(300);
        }
    }

    private void animateFabsOut() {
        if (fabSend != null) {
            fabSend.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabSend.setVisibility(View.GONE));
        }
        if (fabReceive != null) {
            fabReceive.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabReceive.setVisibility(View.GONE));
        }
        if (fabObstacle != null) {
            fabObstacle.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabObstacle.setVisibility(View.GONE));
        }
        if (fabRobot != null) {
            fabRobot.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabRobot.setVisibility(View.GONE));
        }
        if (fabImage != null) {
            fabImage.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabImage.setVisibility(View.GONE));
        }
        if (fabSendLabel != null) {
            fabSendLabel.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabSendLabel.setVisibility(View.GONE));
        }
        if (fabReceiveLabel != null) {
            fabReceiveLabel.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabReceiveLabel.setVisibility(View.GONE));
        }
        if (fabObstacleLabel != null) {
            fabObstacleLabel.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabObstacleLabel.setVisibility(View.GONE));
        }
        if (fabRobotLabel != null) {
            fabRobotLabel.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabRobotLabel.setVisibility(View.GONE));
        }
        if (fabImageLabel != null) {
            fabImageLabel.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(300).withEndAction(() -> fabImageLabel.setVisibility(View.GONE));
        }
    }

    public void showSendDataSection() {
        if (gridTableSection != null) gridTableSection.setVisibility(View.VISIBLE);
        if (sendDataSection != null) sendDataSection.setVisibility(View.VISIBLE);
        if (receiveDataSection != null) receiveDataSection.setVisibility(View.GONE);
        if (obstacleControlSection != null) obstacleControlSection.setVisibility(View.GONE);
        if (robotControlSection != null) robotControlSection.setVisibility(View.GONE);
        if (imageSection != null) imageSection.setVisibility(View.GONE);
        showToast("Send Data Section");
    }

    public void showReceiveDataSection() {
        if (gridTableSection != null) gridTableSection.setVisibility(View.VISIBLE);
        if (sendDataSection != null) sendDataSection.setVisibility(View.GONE);
        if (receiveDataSection != null) receiveDataSection.setVisibility(View.VISIBLE);
        if (obstacleControlSection != null) obstacleControlSection.setVisibility(View.GONE);
        if (robotControlSection != null) robotControlSection.setVisibility(View.GONE);
        if (imageSection != null) imageSection.setVisibility(View.GONE);
        showToast("Receive Data Section");
    }

    public void showObstacleSection() {
        if (gridTableSection != null) gridTableSection.setVisibility(View.VISIBLE);
        if (sendDataSection != null) sendDataSection.setVisibility(View.GONE);
        if (receiveDataSection != null) receiveDataSection.setVisibility(View.GONE);
        if (obstacleControlSection != null) obstacleControlSection.setVisibility(View.VISIBLE);
        if (robotControlSection != null) robotControlSection.setVisibility(View.GONE);
        if (imageSection != null) imageSection.setVisibility(View.GONE);
    }

    public void showRobotSection() {
        if (gridTableSection != null) gridTableSection.setVisibility(View.VISIBLE);
        if (sendDataSection != null) sendDataSection.setVisibility(View.GONE);
        if (receiveDataSection != null) receiveDataSection.setVisibility(View.GONE);
        if (obstacleControlSection != null) obstacleControlSection.setVisibility(View.GONE);
        if (robotControlSection != null) robotControlSection.setVisibility(View.VISIBLE);
        if (imageSection != null) imageSection.setVisibility(View.GONE);
        showToast("Robot panel shown");
    }

    public void showImageSection() {
        if (gridTableSection != null) gridTableSection.setVisibility(View.GONE);
        if (sendDataSection != null) sendDataSection.setVisibility(View.GONE);
        if (receiveDataSection != null) receiveDataSection.setVisibility(View.GONE);
        if (obstacleControlSection != null) obstacleControlSection.setVisibility(View.GONE);
        if (robotControlSection != null) robotControlSection.setVisibility(View.GONE);
        if (imageSection != null) imageSection.setVisibility(View.VISIBLE);
        showToast("Image Section");
    }

    public void hideRobotSection() {
        if (robotControlSection != null) robotControlSection.setVisibility(View.GONE);
        showToast("Robot panel hidden");
    }

    public void updateObstacleButtonState() {
        // Disable toggle semantics; always show consistent icon/label for obstacle mode
        if (fabObstacle != null) {
            fabObstacle.setImageResource(android.R.drawable.ic_delete);
        }
        if (fabObstacleLabel instanceof TextView) {
            ((TextView) fabObstacleLabel).setText("Obstacle Mode");
        }
    }

    public boolean isRobotSectionVisible() {
        return robotControlSection != null && robotControlSection.getVisibility() == View.VISIBLE;
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
