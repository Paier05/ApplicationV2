package com.group1.application.activities

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
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
    private lateinit var scanButton: MaterialButton
    private lateinit var devicesAdapter: BluetoothDeviceAdapter
    private val devices = mutableListOf<BluetoothDevice>()

    private enum class RoleMode { CLIENT, SERVER }
    private var currentMode = RoleMode.CLIENT
    private lateinit var modeToggleButton: MaterialButton

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

        devicesAdapter = BluetoothDeviceAdapter(this, devices)
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
                statusText.text = "Server mode - tap device to wait for connection"
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
            isConnected -> "Connected to Bluetooth device"
            isWaiting -> "Waiting for laptop to connect..."
            helper.isBluetoothEnabled() -> "Bluetooth enabled - Ready to scan"
            else -> "Bluetooth disabled - Please enable Bluetooth manually"
        }
    }

    private fun scanForDevices() {
        if (PermissionUtils.checkAndRequestBluetoothPermissions(this)) {
            statusText.text = "Please grant Bluetooth permissions"
            return
        }

        if (bluetoothHelper?.isBluetoothEnabled() == false) {
            statusText.text = "Please enable Bluetooth manually to scan"
            return
        }

        statusText.text = "Scanning for devices..."
        devices.clear()
        devicesAdapter.notifyDataSetChanged()

        val pairedDevices = bluetoothHelper?.getPairedDevices() ?: emptyList()
        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                if (!devices.any { it.address == device.address }) {
                    devices.add(device)
                }
            }
            devicesAdapter.notifyDataSetChanged()
        }

        bluetoothHelper?.startDiscovery()

        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothHelper?.stopDiscovery()
            statusText.text = "Scan completed. Found ${devices.size} devices"
        }, BluetoothConstants.DISCOVERY_TIMEOUT_MS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BluetoothConstants.REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                updateUI()
            } else {
                statusText.text = "Permissions required for discovery"
            }
        }
    }

    private fun startClientConnection(device: BluetoothDevice) {
        bluetoothHelper?.cleanup()
        bluetoothHelper = BluetoothHelper(this).also {
            it.setConnectionListener(this)
        }
        statusText.text = "Connecting to ${getDeviceName(device)}..."
        bluetoothHelper?.connectToDevice(device)
    }

    private fun startServerWait(device: BluetoothDevice) {
        bluetoothHelper?.cleanup()
        bluetoothHelper = BluetoothHelper(this).also {
            it.setConnectionListener(this)
        }
        statusText.text = "Waiting for connection from ${getDeviceName(device)}..."
        bluetoothHelper?.waitForConnectionFromDevice(device)
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return try {
            if (PermissionUtils.checkBluetoothPermissions(this)) device.name ?: "Unknown Device" else "Unknown Device"
        } catch (e: SecurityException) {
            "Unknown Device"
        }
    }

    private fun getDeviceAddress(device: BluetoothDevice): String = device.address

    override fun onDeviceConnected(device: BluetoothDevice) {
        runOnUiThread {
            val deviceName = getDeviceName(device)
            statusText.text = "Connected to $deviceName"
            bluetoothHelper?.let { DataCommunicationActivity.setBluetoothHelper(it) }
            val intent = Intent(this, DataCommunicationActivity::class.java).apply {
                putExtra(BluetoothConstants.EXTRA_DEVICE_NAME, deviceName)
            }
            startActivity(intent)
        }
    }

    override fun onWaitingForConnection(device: BluetoothDevice) {
        runOnUiThread {
            statusText.text = "Waiting for ${getDeviceName(device)}..."
            updateUI()
        }
    }

    override fun onConnectionTimeout() {
        runOnUiThread {
            statusText.text = "Connection timeout"
            updateUI()
        }
    }

    override fun onDeviceDisconnected() {
        runOnUiThread {
            statusText.text = "Device disconnected"
            updateUI()
        }
    }

    override fun onDataReceived(data: String) {}

    override fun onConnectionFailed(error: String) {
        runOnUiThread {
            statusText.text = "Connection failed: $error"
            updateUI()
        }
    }

    override fun onDeviceDiscovered(device: BluetoothDevice) {
        runOnUiThread {
            if (!devices.any { it.address == device.address }) {
                devices.add(device)
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

    /**
     * Custom adapter for Bluetooth devices to make the list look beautiful
     */
    private inner class BluetoothDeviceAdapter(context: Context, private val deviceList: List<BluetoothDevice>) :
        ArrayAdapter<BluetoothDevice>(context, 0, deviceList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.bluetooth_device_item, parent, false)
            val device = getItem(position)

            val nameText = view.findViewById<TextView>(R.id.deviceName)
            val addressText = view.findViewById<TextView>(R.id.deviceAddress)
            val icon = view.findViewById<ImageView>(R.id.deviceIcon)

            device?.let {
                val name = getDeviceName(it)
                nameText.text = name
                addressText.text = it.address
                
                // If device is paired, maybe show a different icon or indicator
                // For now, use the default bluetooth icon
                icon.setImageResource(android.R.drawable.stat_sys_data_bluetooth)
            }

            return view
        }
    }
}
