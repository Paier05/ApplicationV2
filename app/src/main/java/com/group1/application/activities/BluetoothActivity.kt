package com.group1.application.activities

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.group1.application.R
import com.group1.application.bluetooth.BluetoothConnectionListener
import com.group1.application.bluetooth.BluetoothHelper
import com.group1.application.utils.BluetoothConstants
import com.group1.application.utils.PermissionUtils

class BluetoothActivity : AppCompatActivity(), BluetoothConnectionListener {

    companion object {
        private const val TAG = "BluetoothActivity"
    }

    private var bluetoothHelper: BluetoothHelper? = null
    private lateinit var statusText: TextView
    private lateinit var scanButton: Button
    private lateinit var devicesAdapter: ArrayAdapter<String>
    private val devices = mutableListOf<BluetoothDevice>()

    private enum class RoleMode { CLIENT, SERVER }
    private var currentMode = RoleMode.CLIENT
    private lateinit var modeToggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        initializeViews()
        setupBluetooth()
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        scanButton = findViewById(R.id.scanButton)
        val devicesList = findViewById<ListView>(R.id.devicesList)
        modeToggleButton = findViewById(R.id.modeToggleButton)

        devicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        devicesList.adapter = devicesAdapter

        scanButton.setOnClickListener { scanForDevices() }
        modeToggleButton.setOnClickListener { toggleMode() }

        devicesList.setOnItemClickListener { _, _, position, _ ->
            if (position < devices.size) {
                val device = devices[position]
                if (PermissionUtils.checkAndRequestBluetoothPermissions(this)) {
                    Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
                    return@setOnItemClickListener
                }
                if (currentMode == RoleMode.CLIENT) {
                    startClientConnection(device)
                } else {
                    startServerWait(device)
                }
            }
        }

        devicesList.onItemLongClickListener = null
    }

    private fun toggleMode() {
        if (bluetoothHelper?.isConnected() == true) {
            Toast.makeText(this, "Disconnect first to change mode", Toast.LENGTH_SHORT).show()
            return
        }
        currentMode = if (currentMode == RoleMode.CLIENT) RoleMode.SERVER else RoleMode.CLIENT
        updateModeUI()
    }

    private fun updateModeUI() {
        val helper = bluetoothHelper ?: return
        if (currentMode == RoleMode.CLIENT) {
            modeToggleButton.text = "Mode: Client"
            if (!helper.isConnected() && !helper.isWaitingForConnection()) {
                statusText.text = "Client mode - tap device to connect"
            }
        } else {
            modeToggleButton.text = "Mode: Server"
            if (!helper.isConnected() && !helper.isWaitingForConnection()) {
                statusText.text = "Server mode - tap device to wait for incoming connection"
            }
        }
    }

    private fun setupBluetooth() {
        bluetoothHelper = BluetoothHelper(this).also {
            it.setConnectionListener(this)
        }

        if (bluetoothHelper?.isBluetoothSupported() == false) {
            statusText.text = "Bluetooth not supported on this device"
            scanButton.isEnabled = false
            return
        }

        updateUI()
        updateModeUI()
    }

    private fun updateUI() {
        val helper = bluetoothHelper ?: return
        val isConnected = helper.isConnected()
        val isWaiting = helper.isWaitingForConnection()

        scanButton.isEnabled = !isConnected && !isWaiting

        statusText.text = when {
            isConnected -> "Connected to Bluetooth device - Ready to send/receive data"
            isWaiting -> "Waiting for laptop to connect..."
            helper.isBluetoothEnabled() -> "Bluetooth enabled - Ready to scan"
            else -> "Bluetooth disabled - Please enable Bluetooth manually"
        }
    }

    private fun scanForDevices() {
        Log.d(TAG, "scanForDevices() called")

        if (PermissionUtils.checkAndRequestBluetoothPermissions(this)) {
            statusText.text = "Please grant Bluetooth permissions"
            return
        }

        if (bluetoothHelper?.isBluetoothEnabled() == false) {
            statusText.text = "Please enable Bluetooth manually to scan for devices"
            return
        }

        statusText.text = "Scanning for devices..."
        devices.clear()
        devicesAdapter.clear()
        devicesAdapter.notifyDataSetChanged()

        val pairedDevices = bluetoothHelper?.getPairedDevices() ?: emptyList()
        Log.d(TAG, "Found ${pairedDevices.size} paired devices")

        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                devices.add(device)
                val deviceName = getDeviceName(device)
                val deviceAddress = getDeviceAddress(device)
                devicesAdapter.add("$deviceName (Paired)\n$deviceAddress")
                Log.d(TAG, "Added paired device: $deviceName - $deviceAddress")
            }
            devicesAdapter.notifyDataSetChanged()
        } else {
            Log.w(TAG, "No paired devices found!")
        }

        statusText.text = "Found ${pairedDevices.size} paired devices. Searching for more..."
        bluetoothHelper?.startDiscovery()

        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothHelper?.stopDiscovery()
            val totalDevices = devices.size
            statusText.text = "Scan completed. Found $totalDevices devices total"
            Log.d(TAG, "Discovery completed. Total devices found: $totalDevices")

            if (totalDevices == 0) {
                Log.w(TAG, "No devices found at all - this indicates a permission or Bluetooth issue")
                statusText.text = "No devices found. Check Bluetooth permissions and ensure devices are discoverable."
            }
        }, BluetoothConstants.DISCOVERY_TIMEOUT_MS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BluetoothConstants.REQUEST_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Log.d(TAG, "All permissions granted by user")
                Toast.makeText(this, "Permissions granted. You can now scan for devices.", Toast.LENGTH_SHORT).show()
                updateUI()
            } else {
                Log.w(TAG, "Some permissions denied by user")
                Toast.makeText(this, "Bluetooth permissions are required for device discovery", Toast.LENGTH_LONG).show()
                statusText.text = "Permissions required - Please grant Bluetooth permissions"
            }
        }
    }

    private fun startClientConnection(device: BluetoothDevice) {
        bluetoothHelper?.cleanup()
        bluetoothHelper = BluetoothHelper(this).also {
            it.setConnectionListener(this)
        }
        statusText.text = "Connecting to ${getDeviceName(device)} as client..."
        bluetoothHelper?.connectToDevice(device)
    }

    private fun startServerWait(device: BluetoothDevice) {
        bluetoothHelper?.cleanup()
        bluetoothHelper = BluetoothHelper(this).also {
            it.setConnectionListener(this)
        }
        statusText.text = "Waiting for connection from ${getDeviceName(device)} (server mode)..."
        bluetoothHelper?.waitForConnectionFromDevice(device)
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        if (!PermissionUtils.checkBluetoothPermissions(this)) return "Unknown Device"
        return try {
            device.name ?: "Unknown Device"
        } catch (e: SecurityException) {
            "Unknown Device"
        }
    }

    private fun getDeviceAddress(device: BluetoothDevice): String {
        return try {
            device.address
        } catch (e: SecurityException) {
            "Unknown Address"
        }
    }

    // BluetoothConnectionListener implementation

    override fun onDeviceConnected(device: BluetoothDevice) {
        runOnUiThread {
            val deviceName = getDeviceName(device)
            statusText.text = "Connected to $deviceName"
            Toast.makeText(this, "Connected to $deviceName", Toast.LENGTH_SHORT).show()

            bluetoothHelper?.let { DataCommunicationActivity.setBluetoothHelper(it) }

            val intent = Intent(this, DataCommunicationActivity::class.java).apply {
                putExtra(BluetoothConstants.EXTRA_DEVICE_NAME, deviceName)
            }
            startActivity(intent)
        }
    }

    override fun onWaitingForConnection(device: BluetoothDevice) {
        runOnUiThread {
            val deviceName = getDeviceName(device)
            statusText.text = if (currentMode == RoleMode.SERVER) {
                "Server mode: waiting for $deviceName... (timeout 60s)"
            } else {
                "(Unexpected) Waiting for $deviceName..."
            }
            Toast.makeText(this, "Waiting for $deviceName", Toast.LENGTH_LONG).show()
            updateUI()
        }
    }

    override fun onConnectionTimeout() {
        runOnUiThread {
            statusText.text = "Connection timeout - No incoming connection received"
            Toast.makeText(this, "Connection timeout. Try again.", Toast.LENGTH_LONG).show()
            updateUI()
        }
    }

    override fun onDeviceDisconnected() {
        runOnUiThread {
            statusText.text = "Device disconnected"
            updateUI()
        }
    }

    override fun onDataReceived(data: String) {
        // Handle received data
    }

    override fun onConnectionFailed(error: String) {
        runOnUiThread {
            statusText.text = "Connection failed: $error"
            Toast.makeText(this, "Connection failed: $error", Toast.LENGTH_LONG).show()
            updateUI()
        }
    }

    override fun onDeviceDiscovered(device: BluetoothDevice) {
        runOnUiThread {
            if (!devices.contains(device)) {
                devices.add(device)
                devicesAdapter.add("${getDeviceName(device)}\n${getDeviceAddress(device)}")
                devicesAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHelper?.cleanup()
    }

    override fun onResume() {
        super.onResume()
        bluetoothHelper?.let {
            it.setConnectionListener(this)
            updateUI()
            updateModeUI()
        }
    }
}