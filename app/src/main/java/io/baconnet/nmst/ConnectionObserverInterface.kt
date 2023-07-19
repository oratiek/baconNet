package io.baconnet.nmst

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.observer.ConnectionObserver

interface ConnectionObserverInterface : ConnectionObserver {
    override fun onDeviceConnecting(device: BluetoothDevice) {}

    override fun onDeviceConnected(device: BluetoothDevice) {}

    override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {}

    override fun onDeviceReady(device: BluetoothDevice) {}

    override fun onDeviceDisconnecting(device: BluetoothDevice) {}

    override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {}
}