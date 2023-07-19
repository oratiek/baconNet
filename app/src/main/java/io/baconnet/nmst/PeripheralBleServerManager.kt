package io.baconnet.nmst

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.observer.ServerObserver
import java.util.UUID

class PeripheralBleServerManager(private val context: Context) : BleServerManager(context) {
    companion object {
        const val NMST_UUID = "0f43d388-2ccd-4668-ab5c-5ba40a198261"
    }
    private val nmstUUID = UUID.fromString(NMST_UUID)

    val nmstCharacteristic = characteristic(nmstUUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ, cccd())

    override fun initializeServer() = listOf(service(nmstUUID, nmstCharacteristic))

    private val connectionObserver = object: ConnectionObserverInterface {}
    private val serverObserver = object: ServerObserver {
        override fun onServerReady() {
            startAdvertising()
        }

        override fun onDeviceConnectedToServer(device: BluetoothDevice) {
            deviceConnectedToServer(device)
        }

        override fun onDeviceDisconnectedFromServer(device: BluetoothDevice) {}
    }

    fun deviceConnectedToServer(device: BluetoothDevice) {
        val connectedBleManager = ConnectedBleManager(context)
        connectedBleManager.setConnectionObserver(connectionObserver)
        connectedBleManager.useServer(this)

        Thread {
            while (true) {
                connectedBleManager.notify(nmstCharacteristic, "test".toByteArray())
                Thread.sleep(1000)
            }
        }.start()

        connectedBleManager.connect(device).enqueue()
    }

    fun startAdvertising() {
        val advertiseSettings = AdvertiseSettings.Builder()
            .build()
        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(nmstUUID))
            .setIncludeDeviceName(true)
            .build()
        val advertiseCallback = object : AdvertiseCallback() {}

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

    init {
        setServerObserver(serverObserver)
    }
}