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
import io.baconnet.inspection.ChatGptInspector
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.observer.ServerObserver
import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID

class PeripheralBleServerManager(private val context: Context) : BleServerManager(context) {
    private val nmstUUID = UUID.fromString(context.getString(R.string.nmst_service_uuid))
    private val nmstReceiveUUID = UUID.fromString(context.getString(R.string.nmst_receive_service_uuid))
    private val nmstRequestUUID = UUID.fromString(context.getString(R.string.nmst_request_service_uuid))

    val nmstCharacteristic = characteristic(nmstUUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ, cccd())
    val nmstReceiveCharacteristic = characteristic(nmstReceiveUUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
    val nmstRequestCharacteristic = characteristic(nmstRequestUUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
    private var messageQueue = ArrayDeque<Message>()

    override fun initializeServer() = listOf(service(nmstUUID, nmstCharacteristic, nmstReceiveCharacteristic, nmstRequestCharacteristic))

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

    fun addQueue(message: Message) {
        this.messageQueue.add(message)
    }

    fun deviceConnectedToServer(device: BluetoothDevice) {
        val connectedBleManager = ConnectedBleManager(context)
        connectedBleManager.setConnectionObserver(connectionObserver)
        connectedBleManager.useServer(this)

        var readRequest: String? = null
        connectedBleManager.withWriteCallback(nmstRequestCharacteristic).with { device, data ->
            Log.i("Peripheral", "Received: NMST Request.")
            readRequest = device.address!!
        }

        var nmstBuffer: ByteBuffer? = null
        var nmstReadCount: Int = 0
        connectedBleManager.withWriteCallback(nmstReceiveCharacteristic).with { device, data ->
            if (data.value!![0].toInt() == -2) {
                Log.i("Peripheral", "Received readRequest!")
                readRequest = device.address!!
            } else if (nmstBuffer == null) {
                val buffer = ByteBuffer.wrap(data.value!!)
                if (buffer.get().toInt() == -1) {
                    nmstReadCount = buffer.int
                    nmstBuffer = ByteBuffer.wrap(ByteArray(buffer.int))
                }
            } else if (nmstReadCount != 0) {
                nmstReadCount--
                Log.i("Central", "Receive: ${String(data.value!!)}")
                nmstBuffer?.put(data.value!!)
            } else {
                val data = String(nmstBuffer!!.array())
                val message: Message = Json.decodeFromString(data)
                val messages = (context as MainActivity).getMessages()!!
                messages[message.messageId] = message

                context.nmstClient.central.messageQueue.add(message)
                context.nmstClient.messages.value?.add(message)
                context.setMessages(messages)
                Log.i("NMST", "Messages: ${context.nmstClient.messages}")
                nmstBuffer = null
                Log.i("Central", "Receive: $data")
                Log.i("Central", "Receive: $message")

                context.navigateToTimeline()

                Thread {
                    ChatGptInspector().inspect(context, message.displayName, message.body) {
                        if (it) {
                            val ids = context.getVerifiedMessageIds()
                            ids.add(message.messageId)
                            context.setVerifiedMessageIds(ids)
                        }
                    }
                }.start()
            }
        }

        Thread {
            while (true) {
                if (readRequest != null) {
                    Log.i("Peripheral", "Received readRequest")
                    val messages = run {
                        val messages = (context as MainActivity).nmstClient.messages.value!!
                        messages.filter { message ->
                            (!message.sentDevices.contains(readRequest) && message.sentDevices.count() < 10) || true
                        }
                    }
                    this.messageQueue.addAll(messages)
                    readRequest = null
                }
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
                        Thread.sleep(1)
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