package io.baconnet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.baconnet.nmst.CentralBleServerManager
import io.baconnet.nmst.ConnectionObserverInterface
import io.baconnet.nmst.NmstClient
import io.baconnet.nmst.PeripheralBleServerManager
import io.baconnet.ui.pages.FirstTime
import io.baconnet.ui.pages.Post
import io.baconnet.ui.pages.Settings
import io.baconnet.ui.pages.Timeline
import io.baconnet.ui.theme.Bacon_netTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.util.UUID

class MainActivity : ComponentActivity() {
    lateinit var navController: NavController
    lateinit var nmstClient: NmstClient

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.initDataIfNotExists()

        val requestPermissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN)
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted -> Boolean
            var i = 0
            isGranted.forEach {
                if (!it.value) {
                    requestPermissions[i] = it.key
                    i++
                }
            }
        }
        permissionLauncher.launch(requestPermissions)

        val peripheral = PeripheralBleServerManager(this)
        peripheral.open()
        val central = CentralBleServerManager(this, null, 0, ArrayDeque(), arrayListOf())
        central.setConnectionObserver(object: ConnectionObserverInterface {})
        central.discoveredServicesHandler = { central, gatt, services ->
            val nmstService = services.find {
                it.uuid == UUID.fromString("0f43d388-2ccd-4668-ab5c-5ba40a198261")
            }
            Log.i("Central", "Service discovered.")
            if (nmstService == null) {
                Log.i("Central", "NMST Service not found.")
                central.deviceFilter.add(gatt.device.address)
                central.disconnect().enqueue()
                central.close()
                if (ActivityCompat.checkSelfPermission(
                        central.context(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.disconnect()
                    gatt.close()
                }
            } else {
                Log.i("Central", "NMST Service found.")
                central.notificationCallback(nmstService.getCharacteristic(UUID.fromString("0f43d388-2ccd-4668-ab5c-5ba40a198261"))) { central, device, data ->
                    if (central.nmstBuffer == null) {
                        val buffer = ByteBuffer.wrap(data.value!!)
                        if (buffer.get().toInt() == -1) {
                            central.nmstReadCount = buffer.get().toInt()
                            central.nmstBuffer = ByteBuffer.wrap(ByteArray(buffer.int))
                        }
                    } else if (central.nmstReadCount != 0) {
                        central.nmstReadCount--
                        Log.i("Central", "Receive: ${String(data.value!!)}")
                        central.nmstBuffer?.put(data.value!!)
                    } else {
                        val data = String(central.nmstBuffer!!.array())
                        central.messageQueue.add(Json.decodeFromString(data))
                        nmstClient.messages.value?.add(central.messageQueue.first())
                        Log.i("NMST", "Messages: ${nmstClient.messages}")
                        central.nmstBuffer = null
                        Log.i("Central", "Receive: $data")
                        Log.i("Central", "Receive: ${central.messageQueue.first()}")
                        central.disconnect()
                    }
                }
            }
        }
        nmstClient = NmstClient(this, peripheral, central, MutableLiveData())
        nmstClient.messages.value = mutableListOf()
        nmstClient.messages.postValue(mutableListOf())
        nmstClient.init()

        setContent {
            Bacon_netTheme {
                val activity = LocalContext.current as MainActivity
                val navController = rememberNavController()
                this.navController = navController

                val startDestination =
                    if (activity.getDisplayName() == "") "first_time" else "timeline"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("first_time") { FirstTime() }
                    composable("timeline") {
                        Timeline()
                    }
                    composable("post") { Post() }
                    composable("settings") { Settings() }
                }
            }
        }
    }

    private fun initDataIfNotExists() {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        val encryptionType = pref.getString(getString(R.string.key_encryption_type), "") ?: ""
        val publicKey = pref.getString(getString(R.string.key_public_key), "") ?: ""
        val privateKey = pref.getString(getString(R.string.key_private_key), "") ?: ""
        val displayName = pref.getString(getString(R.string.key_display_name), "") ?: ""

        with(pref.edit()) {

            if (displayName == "") {
                putString(getString(R.string.key_display_name), "")
            }

            if (encryptionType == "") {
                val generator = KeyPairGenerator.getInstance("RSA")
                val pair = generator.genKeyPair()

                putString(getString(R.string.key_encryption_type), "RSA")
                putString(getString(R.string.key_public_key), pair.public.toString())
                putString(getString(R.string.key_private_key), pair.private.toString())
            }

            apply()
        }
    }

    public fun getDisplayName(): String {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        return pref.getString(getString(R.string.key_display_name), "") ?: ""
    }

    public fun setDisplayName(displayName: String) {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putString(getString(R.string.key_display_name), displayName)
            apply()
        }
    }

    public fun getEncryptionType(): String {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        return pref.getString(getString(R.string.key_encryption_type), "") ?: ""
    }

    public fun getPublicKey(): String {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        return pref.getString(getString(R.string.key_public_key), "") ?: ""
    }

    public fun getPrivateKey(): String {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        return pref.getString(getString(R.string.key_private_key), "") ?: ""
    }

    public fun navigateToTimeline() {
        navController.navigate("timeline")
    }


    public fun navigateToPost() {
        navController.navigate("post")
    }

    public fun navigateToSettings() {
        navController.navigate("settings")
    }
}