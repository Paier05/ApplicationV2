package com.group1.application.activities

import android.bluetooth.BluetoothDevice
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.group1.application.R
import com.group1.application.adapters.GridTableAdapter
import com.group1.application.bluetooth.BluetoothConnectionListener
import com.group1.application.bluetooth.BluetoothHelper
import com.group1.application.controllers.FabMenuController
import com.group1.application.controllers.ObstacleController
import com.group1.application.controllers.RobotController
import com.group1.application.managers.DataCommunicationManager
import com.group1.application.parsers.MessageParser
import com.group1.application.utils.BluetoothConstants

class DataCommunicationActivity : AppCompatActivity(),
    BluetoothConnectionListener,
    FabMenuController.OnFabMenuListener,
    ObstacleController.OnObstacleModeChangeListener,
    MessageParser.OnMessageParsedListener {

    companion object {
        private const val TAG = "DataCommunicationActivity"

        private var sharedBluetoothHelper: BluetoothHelper? = null

        @JvmStatic
        fun setBluetoothHelper(helper: BluetoothHelper) {
            sharedBluetoothHelper = helper
        }
    }

    private var bluetoothHelper: BluetoothHelper? = null

    // UI Components
    private lateinit var gridTableHeaderText: TextView
    private lateinit var gridView: GridView
    private lateinit var gridAdapter: GridTableAdapter
    private lateinit var imageContainer: LinearLayout

    // Controllers and Managers
    private lateinit var dataCommunicationManager: DataCommunicationManager
    private lateinit var fabMenuController: FabMenuController
    private lateinit var robotController: RobotController
    private lateinit var obstacleController: ObstacleController
    private var messageParser: MessageParser? = null

    private var connectedDeviceName = "Unknown Device"

    // Message Buffer for Bluetooth fragmentation
    private val incomingMessageBuffer = StringBuilder()

    // Reconnect grace period management (60s)
    private val disconnectHandler = Handler(Looper.getMainLooper())
    private var delayedFinishRunnable: Runnable? = null
    private var isWaitingForReconnect = false
    private var manualDisconnectInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_communication)

        connectedDeviceName = intent.getStringExtra(BluetoothConstants.EXTRA_DEVICE_NAME) ?: "Unknown Device"

        initializeControllers()
        initializeViews()
        setupBluetooth()
        setupBackPressedHandler()
        setupGridTable()
        setupUIComponents()
    }

    private fun initializeControllers() {
        dataCommunicationManager = DataCommunicationManager(this)
        fabMenuController = FabMenuController(this)
        robotController = RobotController(this)
        obstacleController = ObstacleController(this)
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                disconnect()
            }
        })
    }

    private fun initializeViews() {
        gridTableHeaderText = findViewById(R.id.gridTableHeaderText)
        gridView = findViewById(R.id.gridView)
        imageContainer = findViewById(R.id.imageContainer)

        findViewById<Button>(R.id.disconnectButton).setOnClickListener { disconnect() }
    }

    private fun setupUIComponents() {
        // Setup Data Communication Manager
        dataCommunicationManager.setUIComponents(
            findViewById(R.id.connectionStatusText),
            findViewById(R.id.receivedDataText),
            findViewById(R.id.sentDataText),
            findViewById(R.id.messageCountText),
            findViewById(R.id.messageInput),
            findViewById(R.id.sendButton),
            findViewById(R.id.clearButton),
            findViewById(R.id.quickSend1),
            findViewById(R.id.quickSend2),
            findViewById(R.id.quickSend3)
        )

        // Setup FAB Menu Controller
        fabMenuController.initialize(this)
        fabMenuController.setUIComponents(
            findViewById(R.id.fabMain),
            findViewById(R.id.fabSend),
            findViewById(R.id.fabReceive),
            findViewById(R.id.fabObstacle),
            findViewById(R.id.fabRobot),
            findViewById(R.id.fabStart),
            findViewById(R.id.fabImage),
            findViewById(R.id.fabSendLabel),
            findViewById(R.id.fabReceiveLabel),
            findViewById(R.id.fabObstacleLabel),
            findViewById(R.id.fabRobotLabel),
            findViewById(R.id.fabStartLabel),
            findViewById(R.id.fabImageLabel),
            findViewById(R.id.sendDataSection),
            findViewById(R.id.receiveDataSection),
            findViewById(R.id.obstacleControlSection),
            findViewById(R.id.robotControlSection),
            findViewById(R.id.startSection),
            findViewById(R.id.imageSection)
        )

        // Setup Robot Controller
        robotController.setUIComponents(
            findViewById(R.id.placeRobotButton),
            findViewById(R.id.robotConfirmButton),
            findViewById(R.id.robotUpButton),
            findViewById(R.id.robotDownButton),
            findViewById(R.id.robotTurnLeftButton),
            findViewById(R.id.robotTurnRightButton),
            findViewById(R.id.robotStatusText),
            findViewById(R.id.robotTopLeftXInput),
            findViewById(R.id.robotTopLeftYInput),
            findViewById(R.id.placeRobotByCoordButton),
            findViewById(R.id.robotPositionStatusText)
        )

        // Setup Obstacle Controller
        obstacleController.initialize(gridAdapter, bluetoothHelper, this)
        obstacleController.setUIComponents(
            findViewById(R.id.addObstacleButton),
            findViewById(R.id.removeObstacleButton),
            findViewById(R.id.clearAllObstaclesButton),
            findViewById(R.id.confirmObstacleButton),
            findViewById(R.id.cancelObstacleButton),
            findViewById(R.id.obstacleActionStatus),
            findViewById(R.id.borderDirectionSection),
            findViewById(R.id.sendObstaclesButton),
            findViewById(R.id.toggleSendObstaclesButton),
            findViewById(R.id.tempObstacleXInput),
            findViewById(R.id.tempObstacleYInput),
            findViewById(R.id.placeTempObstacleButton),
            findViewById(R.id.tempObstacleStatusText),
            findViewById(R.id.tempObstacleDirectionSpinner)
        )

        findViewById<Button?>(R.id.removeObstacleButton)?.setOnClickListener {
            obstacleController.enterRemoveMode()
        }

        findViewById<Button?>(R.id.startButton)?.setOnClickListener {
            dataCommunicationManager.sendQuickMessage("Start")
        }
    }

    private fun setupGridTable() {
        gridAdapter = GridTableAdapter(this)
        gridView.adapter = gridAdapter

        robotController.initialize(gridAdapter, bluetoothHelper)
        messageParser = MessageParser(gridAdapter, this)

        gridView.setOnItemClickListener { _, _, position, _ ->
            val row = position / gridAdapter.gridSize
            val col = position % gridAdapter.gridSize

            if (col > 0 && row >= 0 && row < gridAdapter.gridSize - 1) {
                if (robotController.handleRobotPlacementClick(row, col)) return@setOnItemClickListener
                obstacleController.handleObstacleClick(row, col)
            }
        }

        gridView.setOnTouchListener { v, event -> handleGridTouch(v, event) }

        Log.d(TAG, "21x21 Grid table initialized successfully")
    }

    private fun handleGridTouch(v: View, event: MotionEvent): Boolean {
        if (robotController.isPlacingRobotMode()) {
            val position = gridView.pointToPosition(event.x.toInt(), event.y.toInt())
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    preventParentIntercept(v, true)
                    if (position != GridView.INVALID_POSITION) {
                        val row = position / gridAdapter.gridSize
                        val col = position % gridAdapter.gridSize
                        if (col > 0 && row >= 0 && row < gridAdapter.gridSize - 1) {
                            robotController.previewRobotAt(row, col)
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    preventParentIntercept(v, false)
                    v.performClick()
                    return true
                }
            }
            return true
        }

        val position = gridView.pointToPosition(event.x.toInt(), event.y.toInt())
        return handleDragLogic(v, event, position)
    }

    private fun handleDragLogic(v: View, event: MotionEvent, position: Int): Boolean {
        if (obstacleController.isObstacleModeEnabled() && obstacleController.currentObstacleAction == "add") {
            val row = position / gridAdapter.gridSize
            val col = position % gridAdapter.gridSize

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    obstacleController.setBatchDragStart(row, col)
                    preventParentIntercept(v, true)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    obstacleController.showDirectionSelectionDialog(
                        obstacleController.batchDragStartRow,
                        obstacleController.batchDragStartCol,
                        row, col
                    )
                    preventParentIntercept(v, false)
                    v.performClick()
                    return true
                }
            }
        }

        if (obstacleController.isDraggingObstacle() && obstacleController.currentObstacleAction == "add") {
            return handleTemporaryObstacleDrag(v, event, position)
        }

        return handlePermanentObstacleDrag()
    }

    private fun handleTemporaryObstacleDrag(v: View, event: MotionEvent, position: Int): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                preventParentIntercept(v, true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                preventParentIntercept(v, true)
                if (isPointerOutsideGrid(event.x, event.y)) {
                    obstacleController.removeTemporaryObstacleFromGrid()
                    return true
                }
                if (position == GridView.INVALID_POSITION) return true

                val row = position / gridAdapter.gridSize
                val col = position % gridAdapter.gridSize
                if (!(col > 0 && row >= 0 && row < gridAdapter.gridSize - 1)) return true

                if (!gridAdapter.isCellObstacle(row, col) || gridAdapter.isCellTemporaryObstacle(row, col)) {
                    if (row != obstacleController.previousTemporaryRow || col != obstacleController.previousTemporaryCol) {
                        obstacleController.updateTemporaryPosition(row, col)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                preventParentIntercept(v, false)
                v.performClick()
                return true
            }
        }
        return false
    }

    private fun isPointerOutsideGrid(x: Float, y: Float): Boolean {
        val left = gridView.paddingLeft
        val top = gridView.paddingTop
        val right = gridView.width - gridView.paddingRight
        val bottom = gridView.height - gridView.paddingBottom
        return x < left || x >= right || y < top || y >= bottom
    }

    private fun handlePermanentObstacleDrag(): Boolean = false

    private fun preventParentIntercept(v: View, prevent: Boolean) {
        v.parent?.requestDisallowInterceptTouchEvent(prevent)
    }

    private fun setupBluetooth() {
        bluetoothHelper = if (sharedBluetoothHelper != null) {
            Log.d(TAG, "Using shared BluetoothHelper instance")
            sharedBluetoothHelper
        } else {
            Log.w(TAG, "Created new BluetoothHelper instance as fallback")
            BluetoothHelper(this)
        }
        bluetoothHelper?.setConnectionListener(this)

        dataCommunicationManager.initialize(bluetoothHelper, connectedDeviceName)

        if (bluetoothHelper?.isConnected() != true) {
            Toast.makeText(this, "Connection lost. Returning to scan.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d(TAG, "Bluetooth connection is active")
        }
    }

    private fun disconnect() {
        manualDisconnectInProgress = true
        bluetoothHelper?.disconnect()
        sharedBluetoothHelper = null
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
        finish()
    }

    // BluetoothConnectionListener implementation

    override fun onDeviceConnected(device: BluetoothDevice) {
        if (isWaitingForReconnect) {
            isWaitingForReconnect = false
            delayedFinishRunnable?.let { disconnectHandler.removeCallbacks(it) }
            delayedFinishRunnable = null
            Toast.makeText(this, "Reconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeviceDisconnected() {
        if (manualDisconnectInProgress) return

        runOnUiThread {
            if (!isWaitingForReconnect) {
                isWaitingForReconnect = true
                Toast.makeText(this, "Device disconnected. Waiting 1 hour for reconnection...", Toast.LENGTH_LONG).show()
                delayedFinishRunnable = Runnable {
                    if (bluetoothHelper?.isConnected() != true) {
                        Toast.makeText(this, "No reconnection. Returning to scan.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        isWaitingForReconnect = false
                    }
                }.also { disconnectHandler.postDelayed(it, 60_000) }
            }
        }
    }

    override fun onDataReceived(data: String) {
        runOnUiThread {
            dataCommunicationManager.onDataReceived()

            // Append new data to buffer
            incomingMessageBuffer.append(data)

            // Extract and process complete lines
            var newlineIndex: Int
            while (incomingMessageBuffer.indexOf("\n").also { newlineIndex = it } != -1) {
                val line = incomingMessageBuffer.substring(0, newlineIndex).trim()
                incomingMessageBuffer.delete(0, newlineIndex + 1)

                if (line.isNotEmpty()) {
                    processIncomingMessage(line)
                }
            }
        }
    }

    private fun processIncomingMessage(message: String) {
        messageParser?.let { parser ->
            parser.parseAndHandleRobotCommands(message)
            parser.parseAndHandleTargetMessages(message)
            parser.parseAndHandleRobotMessages(message)
            parser.parseAndHandleMoveCommand(message)
            parser.parseAndHandleImageMessages(message)

            if (parser.shouldDisplayMessage(message)) {
                dataCommunicationManager.addImportantMessage(message)
            } else {
                Log.d(TAG, "Filtered out message: $message")
            }
        }
    }

    override fun onConnectionFailed(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Connection failed: $error", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDeviceDiscovered(device: BluetoothDevice) {}
    override fun onWaitingForConnection(device: BluetoothDevice) {}
    override fun onConnectionTimeout() {}

    // FabMenuController.OnFabMenuListener implementation

    override fun onSendDataSectionSelected() {
        clearTemporaryObstacle(); exitRobotMode()
        fabMenuController.showSendDataSection()
    }

    override fun onReceiveDataSectionSelected() {
        clearTemporaryObstacle(); exitRobotMode()
        fabMenuController.showReceiveDataSection()
    }

    override fun onObstacleModeToggled() {
        exitRobotMode()
        obstacleController.toggleObstacleMode()
    }

    override fun onRobotPanelToggled() {
        if (fabMenuController.isRobotSectionVisible()) {
            fabMenuController.hideRobotSection()
        } else {
            clearTemporaryObstacle()
            fabMenuController.showRobotSection()
        }
    }

    override fun onStartSectionSelected() {
        clearTemporaryObstacle(); exitRobotMode()
        fabMenuController.showStartSection()
    }

    override fun onImageSectionSelected() {
        clearTemporaryObstacle(); exitRobotMode()
        fabMenuController.showImageSection()
    }

    // ObstacleController.OnObstacleModeChangeListener implementation

    override fun onObstacleModeChanged(enabled: Boolean) {
        fabMenuController.updateObstacleButtonState()
        if (enabled) {
            exitRobotMode()
            fabMenuController.showObstacleSection()
        }
    }

    // MessageParser.OnMessageParsedListener implementation

    override fun onRobotCommandParsed(command: String) {}
    override fun onTargetMessageParsed(obstacleNumber: Int, targetId: String) {}
    override fun onRobotPositionParsed(x: Int, y: Int, direction: String) {}

    override fun onImageReceived(obstacleId: String, imageId: String, imageData: String) {
        try {
            // Clean the image data - remove any potentially problematic whitespace
            val cleanedData = imageData.trim().replace("\\s".toRegex(), "")
            val decodedString = Base64.decode(cleanedData, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            if (decodedByte != null) {
                // Create a new view for this image
                addImageToContainer(obstacleId, imageId, decodedByte)
                
                // Auto switch to image section when image is received
                fabMenuController.showImageSection()
            } else {
                Log.e(TAG, "Failed to decode bitmap from byte array")
                showToast("Received image data is invalid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding image data", e)
            showToast("Failed to decode received image: ${e.message}")
        }
    }

    private fun addImageToContainer(obstacleId: String, imageId: String, bitmap: android.graphics.Bitmap) {
        val context = this
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val textView = TextView(context).apply {
            text = "Obstacle: $obstacleId | Image ID: $imageId"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            gravity = android.view.Gravity.CENTER
        }

        val imageView = ImageView(context).apply {
            setImageBitmap(bitmap)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                800 // height in pixels
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }

        linearLayout.addView(textView)
        linearLayout.addView(imageView)
        
        // Add to the top of the container so latest is first
        imageContainer.addView(linearLayout, 0)
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun getCurrentTimestamp(): String = dataCommunicationManager.currentTimestamp

    override fun appendToReceivedData(message: String) {
        dataCommunicationManager.appendToReceivedData(message)
    }

    override fun appendToSentData(message: String) {
        dataCommunicationManager.appendToSentData(message)
    }

    override fun updateRobotStatusText() {
        robotController.updateRobotStatusText()
    }

    override fun updateGridTableHeader() {
        updateGridTableHeaderText()
    }

    private fun updateGridTableHeaderText() {
        val obsRow = obstacleController.temporaryObstacleRow
        val obsCol = obstacleController.temporaryObstacleCol

        gridTableHeaderText.text = when {
            obstacleController.isDraggingObstacle() && obsRow != -1 && obsCol != -1 -> {
                val displayRow = gridAdapter.gridSize - 2 - obsRow
                val displayCol = obsCol - 1
                "Placing Obstacle at ($displayRow, $displayCol):"
            }
            obstacleController.currentObstacleAction == "add" && obstacleController.isObstacleModeEnabled() ->
                "Add Obstacle Mode - Click on grid:"
            obstacleController.isObstacleModeEnabled() ->
                "Obstacle Mode Active (Index 0-190):"
            else -> ""
        }
    }

    private fun clearTemporaryObstacle() {
        obstacleController.cancelObstacleAction()
        updateGridTableHeaderText()
    }

    private fun exitRobotMode() {
        if (robotController.isPlacingRobotMode()) {
            robotController.exitPlacementMode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        delayedFinishRunnable?.let { disconnectHandler.removeCallbacks(it) }
        delayedFinishRunnable = null
        bluetoothHelper?.cleanup()
        sharedBluetoothHelper = null
    }
}
