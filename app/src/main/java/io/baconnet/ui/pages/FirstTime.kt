package io.baconnet.ui.pages

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import io.baconnet.MainActivity
import io.baconnet.R
import io.baconnet.ui.components.GradientButton
import io.baconnet.ui.theme.BaconGradient
import io.baconnet.ui.theme.BaconPink

@ExperimentalMaterial3Api
@OptIn(ExperimentalTextApi::class)
@Composable
@Preview
fun FirstTime() {
    var activity = LocalContext.current as MainActivity
    var displayName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    val navController = rememberNavController()

    fun handleStartClick() {
        errorText = ""
        if (displayName == "") {
            errorText = "入力してください"
            return@handleStartClick
        }

        activity.setDisplayName(displayName)
        activity.navigateToTimeline()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.baconnet_logo),
            contentDescription = "baconnet logo",
            modifier = Modifier.padding(top = 64.dp, bottom = 16.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BaconNet", style = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    brush = BaconGradient
                )
            )
            Text(
                text = "BLE を用いた近距離\nソーシャルネットワーク",
                style = TextStyle(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            )
        }
        Column(
            Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                value = displayName,
                onValueChange = { newVal -> displayName = newVal.trim().take(8) },
                label = { Text(text = "表示名(8文字まで)") },
                supportingText = {
                    if (errorText.isNotEmpty()) {
                        Text(text = errorText, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Black,
                    focusedLabelColor = Color.Black,
                ),
            )

            GradientButton(text = "始める", onClick = {
                handleStartClick()
            })
        }
    }
}
