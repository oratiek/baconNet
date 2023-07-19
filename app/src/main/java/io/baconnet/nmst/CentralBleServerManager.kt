package io.baconnet.nmst

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer

class CentralBleServerManager(context: Context, var nmstBuffer: ByteBuffer?, var nmstReadCount: Int, var messageQueue: ArrayDeque<Message>) : BleManager(context) {
    var discoveredServicesHandler: ((CentralBleServerManager, BluetoothGatt, List<BluetoothGattService>) -> Unit)? = null

    private var gattCallback: BleManagerGattCallback? = null

    private inner class CentralBleManagerGattCallback: BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            Log.i("Central", "isRequiredServiceSupported()")
            discoveredServicesHandler?.apply { this(this@CentralBleServerManager, gatt, gatt.services) }
            return true
        }

        override fun onDeviceDisconnected() {}
    }

    override fun getGattCallback() = gattCallback ?: run {
        gattCallback = CentralBleManagerGattCallback()
        gattCallback!!
    }

    fun context() = context

    fun notificationCallback(characteristic: BluetoothGattCharacteristic, callback: (Data) -> Unit) {
        setNotificationCallback(characteristic).with { _, data -> callback(data) }
        enableNotifications(characteristic).enqueue()
    }
}