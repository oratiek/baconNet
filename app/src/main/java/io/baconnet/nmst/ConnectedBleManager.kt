package io.baconnet.nmst

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ValueChangedCallback

class ConnectedBleManager(context: Context) : BleManager(context) {
    private var gattCallback: BleManagerGattCallback? = null
    private inner class GattCallback : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean = true
        override fun onDeviceDisconnected() {}
    }

    override fun getGattCallback() = gattCallback ?: run {
        gattCallback = GattCallback()
        gattCallback!!
    }

    fun notify(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        this.sendNotification(characteristic, data).enqueue()
    }

    fun withWriteCallback(serverCharacteristic: BluetoothGattCharacteristic): ValueChangedCallback {
        return super.setWriteCallback(serverCharacteristic)
    }
}