package io.baconnet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.baconnet.ui.theme.BaconGradient

@Composable
fun GradientButton(text: String) {
    var shape = RoundedCornerShape(999.dp);

    Button(
        onClick = { /*TODO*/ },
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(),
        colors = buttonColors(containerColor = Color.Transparent),
        shape = shape,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(brush = BaconGradient, shape = shape)
                .clip(shape)
                .padding(horizontal = 16.dp, vertical = 16.dp), contentAlignment = Alignment.Center
        ) {
            Text(text, style = TextStyle(fontWeight = FontWeight.Bold))
        }
    }
}

@Preview
@Composable
fun GradientButtonPreview() {
    GradientButton("TEXT")
}
