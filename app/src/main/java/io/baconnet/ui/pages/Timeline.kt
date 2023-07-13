package io.baconnet.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import io.baconnet.MainActivity
import io.baconnet.nmst.NmstClient
import io.baconnet.ui.components.PostCard
import io.baconnet.ui.theme.BaconPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Timeline() {
    var client = NmstClient(LocalContext.current)
    val activity = LocalContext.current as MainActivity

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Image(painter = painterResource(id = R.drawable.baconnet_logo_flat_outline), contentDescription = "baconnet logo")
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { activity.navigateToPost() }, containerColor = BaconPink) {
                Icon(painter = rememberVectorPainter(image = Icons.Default.Send), contentDescription = "send icon", tint = Color.White)
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
            }
        }
    }
}

@Preview
@Composable
fun TimelinePreview() {
    Timeline()
}
