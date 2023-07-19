package io.baconnet.nmst

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Message(public val displayName: String, public val userId: String, public val messageId: String, public val body: String, public val postedAt: Instant) {

}