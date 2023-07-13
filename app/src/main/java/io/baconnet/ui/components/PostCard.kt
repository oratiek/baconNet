package io.baconnet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.baconnet.ui.helpers.GetDateAgo
import io.baconnet.ui.theme.BaconGradient
import java.util.Date

@Composable
fun PostCard(
    displayName: String = "<no_name>",
    body: String = "<no_body>",
    isVerified: Boolean = false,
    postedAt: Date = Date()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(all = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(brush = BaconGradient)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName, style = TextStyle(
                            fontWeight = FontWeight.Normal
                        )
                    )
                    Text(
                        text = GetDateAgo(postedAt, Date()),
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray,
                        )
                    )
                }
                if (isVerified) {
                    VerifiedBadge()
                }
                Text(
                    text = body, style = TextStyle(
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun PostCardPreview() {
    PostCard()
}
