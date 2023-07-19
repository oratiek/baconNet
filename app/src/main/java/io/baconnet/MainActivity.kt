package io.baconnet

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.baconnet.nmst.NmstClient
import io.baconnet.ui.pages.FirstTime
import io.baconnet.ui.pages.Post
import io.baconnet.ui.pages.Settings
import io.baconnet.ui.pages.Timeline
import io.baconnet.ui.theme.Bacon_netTheme
import java.security.KeyPairGenerator

class MainActivity : ComponentActivity() {
    lateinit var navController: NavController

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

        val nmst_client = NmstClient(applicationContext)
        nmst_client.init()

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