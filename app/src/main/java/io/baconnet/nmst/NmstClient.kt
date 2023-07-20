package io.baconnet.nmst

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.MutableState
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Date
import java.util.UUID

enum class SubscribeErrorCode {

}

enum class SendErrorCode {}

class NmstClient(private val context: Context, val peripheral: PeripheralBleServerManager, val central: CentralBleServerManager, var messages: MutableLiveData<MutableList<Message>>) {
    companion object {
        const val VERSION = 0
        const val LOG_TAG = "NMST"
        const val SERVICE_UUID = "e9667e2a-e2d9-4b35-872d-fe6d5f086b81"
        private const val SCAN_PERIOD: Long = 10000
    }

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private var scanHandler: Handler? = null
    private val leDevices = mutableListOf<BluetoothDevice>()
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    throw RuntimeException("BLUETOOTH_CONNECT is not permitted.")
                }

                if (result.scanRecord?.serviceUuids?.contains(ParcelUuid.fromString("0f43d388-2ccd-4668-ab5c-5ba40a198261")) == true) {
                    central.connect(device).enqueue()
                }
            }
        }
    }
    private var currentAdvertisingSet: AdvertisingSet? = null

    /**
     * NmstClientを初期化する
     *
     * このメソッドはアプリケーション起動時に一度だけ呼び出すこと
     *
     * @return このメソッドは値を返さない
     */
    fun init(): Unit {
        Log.i(LOG_TAG, "Current SDK version: ${Build.VERSION.SDK_INT}")

        bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        //setupAdvertising()
        scanLeDevice()
    }

    /**
     * メッセージを受信する
     *
     * キューにメッセージが存在しない場合はnullを返す
     *
     * @return 受信したメッセージ
     */
    fun receive(): Message? {
        return central.messageQueue.removeFirstOrNull()
    }

    /**
     * メッセージを送信する
     *
     * @return このメソッドは値を返さない
     */
    fun send(msg: Message): Unit {
        Log.i(LOG_TAG, "Send message to ${msg.displayName}, body: ${msg.body}")
        peripheral.addQueue(msg)
    }

    private fun setupAdvertising() {
        if (Build.VERSION.SDK_INT <= 30) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                throw RuntimeException("BLUETOOTH is not permitted.")
            }
        }
        else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                throw RuntimeException("BLUETOOTH_ADVERTISE is not permitted.")
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                throw RuntimeException("BLUETOOTH_CONNECT is not permitted.")
            }
        }

        if (!bluetoothAdapter.isLe2MPhySupported) {
            throw RuntimeException("2M PHY is not supported.")
        }
        if (!bluetoothAdapter.isLeExtendedAdvertisingSupported) {
            throw RuntimeException("BLE extended advertising is not supported.")
        }

        val params = AdvertisingSetParameters
            .Builder()
            .setLegacyMode(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
            .build()
        val data = AdvertiseData
            .Builder()
            .setIncludeDeviceName(true)
            .build()
        val callback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                super.onAdvertisingSetStarted(advertisingSet, txPower, status)
                Log.i(LOG_TAG, "onAdvertisingSetStarted(): txPower: $txPower, status: $status")
                currentAdvertisingSet = advertisingSet
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet?, status: Int) {
                super.onAdvertisingDataSet(advertisingSet, status)
                Log.i(LOG_TAG, "onAdvertisingDataSet(): status: $status")
            }

            override fun onScanResponseDataSet(advertisingSet: AdvertisingSet?, status: Int) {
                super.onScanResponseDataSet(advertisingSet, status)
                Log.i(LOG_TAG, "onScanResponseDataSet(): status: $status")
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                super.onAdvertisingSetStopped(advertisingSet)
                Log.i(LOG_TAG, "onAdvertisingSetStopped():")
            }
        }

        bluetoothLeAdvertiser.startAdvertisingSet(params, data, null, null, null, callback)
        currentAdvertisingSet?.let {
            it.setAdvertisingData(
                AdvertiseData
                    .Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(true)
                    .build()
            )
            it.setScanResponseData(
                AdvertiseData
                    .Builder()
                    .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
                    .build()
            )
        }
        bluetoothLeAdvertiser.stopAdvertisingSet(callback)
    }

    private fun scanLeDevice() {
        if (scanHandler == null) {
            if (Build.VERSION.SDK_INT <= 30) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    throw RuntimeException("BLUETOOTH is not permitted.")
                }
            }
            else {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    throw RuntimeException("BLUETOOTH_SCAN is not permitted.")
                }
            }

            scanHandler = Handler(Looper.getMainLooper())
            scanHandler?.let {
                Log.i(LOG_TAG, "startScan():")
                bluetoothLeScanner.startScan(leScanCallback)
            }
        }
    }
}