package io.baconnet.ui.pages

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Timeline() {
    val activity = LocalContext.current as MainActivity
    val messagesState = activity.nmstClient.messages.observeAsState(initial = emptyList())

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
                val verifiedIds = activity.getVerifiedMessageIds()
                val messages = activity.getMessages()!!.values.sortedBy {
                    it.postedAt
                }
                messages.reversed().forEach { message ->
                    PostCard(body = message.body, displayName = message.displayName, postedAt = message.postedAt, isVerified = verifiedIds.contains(message.messageId), emailHash = message.emailHash ?: "31c5543c1734d25c7206f5fd591525d0295bec6fe84ff82f946a34fe970a1e66")
                }
            }
        }
    }
}

@Preview
@Composable
fun TimelinePreview() {
    Timeline()
}
