package io.baconnet.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.baconnet.MainActivity
import io.baconnet.nmst.Message
import io.baconnet.nmst.MessageType
import io.baconnet.ui.theme.BaconPink
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Post() {
    var body by remember { mutableStateOf("") }
    var activity = LocalContext.current as MainActivity

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = BaconPink),
                navigationIcon = {
                    IconButton(onClick = { activity.navigateToTimeline() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "back icon",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = "投稿する",
                        style = TextStyle(color = Color.White, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                actions = {
                    IconButton(onClick = {
                        val message = Message.newMessage(body, MessageType.Default, activity)
                        activity.nmstClient.send(message)
                        activity.nmstClient.messages.value?.add(message)
                        val messages = activity.getMessages()!!
                        messages[message.messageId] = message
                        activity.setMessages(messages)
                        activity.navigateToTimeline()
                    }) {
                        Icon(
                            Icons.Filled.Send, contentDescription = "send icon", tint = Color.White
                        )
                    }
                }
            )
        }) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            Box(modifier = Modifier.padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = body,
                        label = {
                            Text(text = "投稿内容")
                        },
                        supportingText = {
                            Text(text = "${body.length} / 2000")
                        },
                        onValueChange = { newVal -> body = newVal.trim().take(2000) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        colors = TextFieldDefaults.textFieldColors(
                        ),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PostPreview() {
    Post()
}