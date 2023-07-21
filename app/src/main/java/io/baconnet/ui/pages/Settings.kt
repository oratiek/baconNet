package io.baconnet.ui.pages

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.baconnet.MainActivity
import io.baconnet.ui.theme.BaconPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings() {
    val activity = LocalContext.current as MainActivity

    var canSave by remember {
        mutableStateOf(false)
    }
    var oldDisplayName = activity.getDisplayName()
    var displayName by remember { mutableStateOf(oldDisplayName) }
    var displayNameError by remember { mutableStateOf("") }

    var oldOpenAIKey = activity.getOpenAIKey()
    var openAIKey by remember { mutableStateOf(oldOpenAIKey) }
    var openAIKeyError by remember { mutableStateOf("") }

    var oldEmail = activity.getEmail()
    var email by remember { mutableStateOf(oldEmail) }

    fun handleChanges() {
        displayNameError = ""

        if (displayName.isEmpty() && displayNameError.isEmpty()) {
            displayNameError = "入力してください"
        }

        if (displayNameError != "") {
            canSave = false;
        } else if (oldDisplayName != displayName) {
            canSave = true
        }
    }

    fun handleSaveClick() {
        activity.setOpenAIKey(openAIKey)
        oldOpenAIKey = activity.getOpenAIKey()

        if (email != null && email != "") {
            activity.setEmail(email!!)
            oldEmail = activity.getEmail()
        }

        if (!canSave) {
            return
        }

        canSave = false

        activity.setDisplayName(displayName)

        oldDisplayName = activity.getDisplayName()
    }

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
                        text = "設定",
                        style = TextStyle(color = Color.White, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    Button(
                        onClick = { handleSaveClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x20FFFFFF))
                    ) {
                        Text(text = "保存")
                    }
                }
            )
        }

    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "表示名", style = TextStyle(fontWeight = FontWeight.Bold))
                        Text(text = "タイムライン上に表示される名前です。")
                        TextField(
                            value = displayName,
                            onValueChange = { newVal ->
                                displayName = newVal.trim().take(8)
                                handleChanges()
                            },
                            label = {
                                Text(text = "表示名(8文字まで)")
                            },
                            supportingText = {
                                if (displayNameError.isNotEmpty()) {
                                    Text(
                                        text = displayNameError,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "公開鍵", style = TextStyle(fontWeight = FontWeight.Bold))
                        Text(text = "ユーザーIDに使われる公開鍵です。")
                        TextField(
                            value = Base64.encodeToString(activity.getPublicKey()!!.encoded, Base64.DEFAULT),
                            onValueChange = {},
                            label = {
                                Text(text = "公開鍵(Base64)")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "秘密鍵", style = TextStyle(fontWeight = FontWeight.Bold))
                        Text(text = "メッセージの署名に使われる秘密鍵です。")
                        TextField(
                            value = Base64.encodeToString(activity.getPrivateKey()!!.encoded, Base64.DEFAULT),
                            onValueChange = {},
                            label = {
                                Text(text = "秘密鍵(Base64)")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "OpenAI API Key", style = TextStyle(fontWeight = FontWeight.Bold))
                        Text(text = "メッセージの検閲に使用するOpenAI Keyです。")
                        TextField(
                            value = openAIKey,
                            onValueChange = { newVal ->
                                openAIKey = newVal.trim().take(64)
                                handleChanges()
                            },
                            label = {
                                Text(text = "OpenAI API Key")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "Email", style = TextStyle(fontWeight = FontWeight.Bold))
                        Text(text = "Gravatarでアイコンを表示するためのメールアドレスです。")
                        TextField(
                            value = email ?: "",
                            onValueChange = { newVal ->
                                email = newVal.trim().take(64)
                                handleChanges()
                            },
                            label = {
                                Text(text = "Email")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item{
                Button(onClick = {
                    activity.setMessages(hashMapOf())
                    activity.nmstClient.messages.value = mutableListOf()
                }) {
                    Text(text = "TLの初期化")
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsPreview() {
    Settings()
}
