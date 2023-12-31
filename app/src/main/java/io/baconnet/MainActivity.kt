package io.baconnet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
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
import io.baconnet.inspection.ChatGptInspector
import io.baconnet.nmst.Message
import io.baconnet.nmst.CentralBleServerManager
import io.baconnet.nmst.ConnectionObserverInterface
import io.baconnet.nmst.NmstClient
import io.baconnet.nmst.PeripheralBleServerManager
import io.baconnet.nmst.splitByteArray
import io.baconnet.ui.pages.FirstTime
import io.baconnet.ui.pages.Post
import io.baconnet.ui.pages.Settings
import io.baconnet.ui.pages.Timeline
import io.baconnet.ui.theme.Bacon_netTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
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
        val central = CentralBleServerManager(this, null, 0, ArrayDeque(), arrayListOf())
        central.setConnectionObserver(object: ConnectionObserverInterface {})
        central.discoveredServicesHandler = { central, gatt, services ->
            val nmstService = services.find {
                it.uuid == UUID.fromString(central.context().getString(R.string.nmst_service_uuid))
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
                Thread {
                    val requestCharacteristic = nmstService.getCharacteristic(UUID.fromString(central.context().getString(R.string.nmst_request_service_uuid)))
                    val receiveCharacteristic = nmstService.getCharacteristic(UUID.fromString(central.context().getString(R.string.nmst_receive_service_uuid)))
                    Thread.sleep(1000)
                    central.write(receiveCharacteristic, run {
                        val byteBuffer = ByteBuffer.allocate(1)
                        byteBuffer.put(0xFE.toByte())
                        byteBuffer.array()
                    })
                    val messages = nmstClient.messages.value!!
                    central.messageQueue.addAll(messages)
                    Thread.sleep(3)
                    while (true) {
                        if (!central.messageQueue.isEmpty()) {
                            val message = central.messageQueue.removeFirst()

                            val data = Json.encodeToString(message).toByteArray()
                            val chunks = splitByteArray(data)

                            central.write(receiveCharacteristic, run {
                                val byteBuffer = ByteBuffer.allocate(9)
                                byteBuffer.put(0xFF.toByte())
                                byteBuffer.putInt(chunks.size)
                                byteBuffer.putInt(data.size)
                                byteBuffer.array()
                            })

                            chunks.forEach {
                                central.write(receiveCharacteristic, it)
                                Thread.sleep(1)
                            }

                            central.write(receiveCharacteristic, ByteArray(10))
                        }

                        Thread.sleep(3)
                    }
                }.start()
                central.notificationCallback(nmstService.getCharacteristic(UUID.fromString("0f43d388-2ccd-4668-ab5c-5ba40a198261"))) { central, device, data ->
                    if (central.nmstBuffer == null) {
                        val buffer = ByteBuffer.wrap(data.value!!)
                        if (buffer.get().toInt() == -1) {
                            central.nmstReadCount = buffer.int
                            central.nmstBuffer = ByteBuffer.wrap(ByteArray(buffer.int))
                        }
                    } else if (central.nmstReadCount != 0) {
                        central.nmstReadCount--
                        Log.i("Central", "Receive: ${String(data.value!!)}")
                        central.nmstBuffer?.put(data.value!!)
                    } else {
                        val data = String(central.nmstBuffer!!.array())
                        val message: Message = Json.decodeFromString(data)
                        val messages = this.getMessages()!!
                        messages[message.messageId] = message

                        central.messageQueue.add(message)
                        nmstClient.messages.value?.add(message)
                        this.setMessages(messages)
                        Log.i("NMST", "Messages: ${nmstClient.messages}")
                        central.nmstBuffer = null
                        Log.i("Central", "Receive: $data")
                        Log.i("Central", "Receive: $message")

                        this.navigateToTimeline()

                        Thread {
                            ChatGptInspector().inspect(this, message.displayName, message.body) {
                                if (it) {
                                    val ids = this.getVerifiedMessageIds()
                                    ids.add(message.messageId)
                                    this.setVerifiedMessageIds(ids)
                                }
                            }
                        }.start()
                    }
                }
            }
        }
        nmstClient = NmstClient(this, peripheral, central, MutableLiveData())
        val mutableCollection = mutableListOf<Message>()
        mutableCollection.addAll(this.getMessages()!!.values)
        nmstClient.messages.value = mutableCollection
        nmstClient.messages.postValue(mutableCollection)
        nmstClient.init()
        peripheral.open()

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
        val messages = pref.getString(getString(R.string.key_messages), "") ?: ""

        with(pref.edit()) {

            if (displayName == "") {
                putString(getString(R.string.key_display_name), "")
            }

            if (encryptionType == "") {
                val generator = KeyPairGenerator.getInstance("RSA")
                val pair = generator.genKeyPair()

                putString(getString(R.string.key_encryption_type), "RSA")
                putString(getString(R.string.key_public_key), Base64.encodeToString(pair.public.encoded, Base64.DEFAULT))
                putString(getString(R.string.key_private_key), Base64.encodeToString(pair.private.encoded, Base64.DEFAULT))
            }

            if (messages == "") {
                putString(getString(R.string.key_messages), Json.encodeToString(hashMapOf<String, Message>()))
            }

            apply()
        }
    }

    fun getVerifiedMessageIds(): ArrayList<String> {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        val messages = pref.getString(getString(R.string.key_verified_message_ids), "[]") ?: return arrayListOf()

        return Json.decodeFromString(messages)
    }

    fun setVerifiedMessageIds(ids: ArrayList<String>) {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putString(getString(R.string.key_verified_message_ids), Json.encodeToString(ids))
            apply()
        }
    }

    fun getMessages(): HashMap<String, Message>? {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        val messages = pref.getString(getString(R.string.key_messages), "") ?: return null

        return Json.decodeFromString(messages)
    }

    fun setMessages(messages: HashMap<String, Message>) {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putString(getString(R.string.key_messages), Json.encodeToString(messages))
            apply()
        }
    }

    public fun getEmail(): String? {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        return pref.getString(getString(R.string.key_email), "")
    }

    public fun setEmail(email: String) {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putString(getString(R.string.key_email), email)
            apply()
        }
    }

    public fun getOpenAIKey(): String {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        return pref.getString(getString(R.string.key_openai_key), "") ?: ""
    }

    public fun setOpenAIKey(openAIKey: String) {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putString(getString(R.string.key_openai_key), openAIKey)
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

    public fun getPublicKey(): PublicKey? {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        val publicKey = pref.getString(getString(R.string.key_public_key), "") ?: return null
        val spec = X509EncodedKeySpec(Base64.decode(publicKey, Base64.DEFAULT))
        val factory = KeyFactory.getInstance("RSA")
        return factory.generatePublic(spec)
    }

    public fun getPrivateKey(): PrivateKey? {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        val publicKey = pref.getString(getString(R.string.key_private_key), "") ?: return null
        val spec = PKCS8EncodedKeySpec(Base64.decode(publicKey, Base64.DEFAULT))
        val factory = KeyFactory.getInstance("RSA")
        return factory.generatePrivate(spec)
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