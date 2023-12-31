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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.baconnet.ui.helpers.GetDateAgo
import io.baconnet.ui.theme.BaconGradient
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.security.MessageDigest
import java.util.Date
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.baconnet.ui.theme.BaconPink

@Composable
fun PostCard(
    displayName: String = "<no_name>",
    body: String = "<no_body>",
    isVerified: Boolean = false,
    isLiked: Boolean = false,
    likes: Int = 0,
    emailHash: String = "31c5543c1734d25c7206f5fd591525d0295bec6fe84ff82f946a34fe970a1e66",
    postedAt: Instant = Clock.System.now(),
    onLikeStateChanged: (isLiked: Boolean) -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(isLiked) }

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

                AsyncImage(
                    model = "https://s.gravatar.com/avatar/$emailHash",
                    contentDescription = "Translated description of what the image contains",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName, style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = GetDateAgo(Date.from(postedAt.toJavaInstant()), Date()),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isLiked) {
                        IconButton(onClick = { isLiked = false; onLikeStateChanged(false); }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = "unlike", tint = BaconPink)
                        }
                    } else {
                        IconButton(onClick = { isLiked = true; onLikeStateChanged(true); }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "like")
                        }
                    }
                    Text(text = " ${likes}")
                }
            }
        }
    }
}

@Preview
@Composable
fun PostCardPreview() {
    PostCard()
}
