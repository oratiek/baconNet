package io.baconnet.nmst

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import no.nordicsemi.android.ble.BleManager

class CentralBleServerManager(context: Context) : BleManager(context) {
    var discoveredServicesHandler: ((BluetoothGatt, List<BluetoothGattService>) -> Unit)? = null

    private var gattCallback: BleManagerGattCallback? = null

    private inner class CentralBleManagerGattCallback: BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            discoveredServicesHandler?.apply { this(gatt, gatt.services) }
            return true
        }

        override fun onDeviceDisconnected() {}

    }

    override fun getGattCallback() = gattCallback ?: run {
        gattCallback = CentralBleManagerGattCallback()
        gattCallback!!
    }
}