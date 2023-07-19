package io.baconnet.ui.pages

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import io.baconnet.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import io.baconnet.MainActivity
import io.baconnet.nmst.CentralBleServerManager
import io.baconnet.nmst.ConnectionObserverInterface
import io.baconnet.nmst.Message
import io.baconnet.nmst.NmstClient
import io.baconnet.nmst.PeripheralBleServerManager
import io.baconnet.ui.components.PostCard
import io.baconnet.ui.theme.BaconPink
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Timeline() {
    val peripheral = PeripheralBleServerManager(LocalContext.current)
    peripheral.open()
    val central = CentralBleServerManager(LocalContext.current, null, 0, ArrayDeque())
    central.setConnectionObserver(object: ConnectionObserverInterface {})
    central.discoveredServicesHandler = { central, gatt, services ->
        val nmstService = services.find {
            it.uuid == UUID.fromString("0f43d388-2ccd-4668-ab5c-5ba40a198261")
        }
        Log.i("Central", "Service discovered.")
        if (nmstService == null) {
            if (ActivityCompat.checkSelfPermission(
                    central.context(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gatt.disconnect()
            }
        } else {
            Log.i("Central", "NMST Service found.")
            central.notificationCallback(nmstService.getCharacteristic(UUID.fromString("0f43d388-2ccd-4668-ab5c-5ba40a198261"))) {
                if (central.nmstBuffer == null) {
                    Log.i("Central", "Receive: ${it.value!![1].toInt()}")
                    if (it.value!![2].toInt() == -1) {
                        central.nmstBuffer = ByteBuffer.wrap(ByteArray(it.value!![1].toInt()))
                        central.nmstReadCount = it.value!![0].toInt()
                    }
                } else if (central.nmstReadCount != 0) {
                    central.nmstReadCount--
                    Log.i("Central", "Receive: ${String(it.value!!)}")
                    central.nmstBuffer?.put(it.value!!)
                } else {
                    val data = String(central.nmstBuffer!!.array())
                    central.nmstBuffer = null
                    Log.i("Central", "Receive: $data")
                    Log.i("Central", "Receive: ${Json.decodeFromString<Message>(data)}")
                }
            }
        }
    }
    val client = NmstClient(LocalContext.current, peripheral, central)
    client.init()

    val activity = LocalContext.current as MainActivity

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Image(
                    painter = painterResource(id = R.drawable.baconnet_logo_flat_outline),
                    contentDescription = "baconnet logo"
                )
            }, actions = {
                IconButton(onClick = { activity.navigateToSettings() }) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "settings icon",
                        tint = Color.Gray
                    )
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { activity.navigateToPost() },
                containerColor = BaconPink
            ) {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Default.Send),
                    contentDescription = "send icon",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            item {
                PostCard()
                PostCard(isVerified = true)
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                PostCard()
                Box(modifier = Modifier.padding(vertical = 128.dp))
            }
        }
    }
}

@Preview
@Composable
fun TimelinePreview() {
    Timeline()
}
