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
import android.util.Log
import androidx.core.app.ActivityCompat
import io.baconnet.MainActivity
import io.baconnet.R
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.observer.ServerObserver
import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID

class PeripheralBleServerManager(private val context: Context) : BleServerManager(context) {
    private val nmstUUID = UUID.fromString(context.getString(R.string.nmst_service_uuid))

    val nmstCharacteristic = characteristic(nmstUUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ, cccd())
    private var messageQueue = ArrayDeque<Message>()

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

    fun splitByteArray(input: ByteArray): List<ByteArray> {
        val chunkSize = 20
        val chunks = mutableListOf<ByteArray>()

        var index = 0
        while (index < input.size) {
            val endIndex = kotlin.math.min(index + chunkSize, input.size)
            val chunk = input.sliceArray(index until endIndex)
            chunks.add(chunk)
            index = endIndex
        }

        return chunks
    }

    fun addQueue(message: Message) {
        this.messageQueue.add(message)
    }

    fun deviceConnectedToServer(device: BluetoothDevice) {
        val connectedBleManager = ConnectedBleManager(context)
        connectedBleManager.setConnectionObserver(connectionObserver)
        connectedBleManager.useServer(this)

        Thread {
            this.messageQueue.addAll((context as MainActivity).nmstClient.messages.value!!)
            Log.i("Peripheral", "Queued messages: ${context.nmstClient.messages.value!!}")
            Thread.sleep(1000)
            while (true) {
                if (!this.messageQueue.isEmpty()) {
                    val message = this.messageQueue.removeFirst()

                    val data = Json.encodeToString(message).toByteArray()
                    val chunks = splitByteArray(data)

                    connectedBleManager.notify(nmstCharacteristic, run {
                        val byteBuffer = ByteBuffer.allocate(9)
                        byteBuffer.put(0xFF.toByte())
                        byteBuffer.putInt(chunks.size)
                        byteBuffer.putInt(data.size)
                        byteBuffer.array()
                    })

                    chunks.forEach {
                        connectedBleManager.notify(nmstCharacteristic, it)
                        Thread.sleep(3)
                    }

                    connectedBleManager.notify(nmstCharacteristic, ByteArray(3))
                }

                Thread.sleep(3)
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