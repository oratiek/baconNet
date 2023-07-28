package io.baconnet.nmst

import android.content.Context
import android.util.Base64
import io.baconnet.MainActivity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

enum class MessageType {
    Default,
    Like
}

@Serializable
data class Message(val messageType: MessageType, public val displayName: String, public val userId: String, val emailHash: String?, public val messageId: String, public val body: String, public val postedAt: Instant, public val sign: String, val sentDevices: ArrayList<String>) {
    companion object {
        fun newMessage(body: String, type: MessageType, context: Context): Message {
            val activity = context as MainActivity
            val displayName = activity.getDisplayName()
            val email = activity.getEmail() ?: "example@example.com"
            val postedAt = Clock.System.now()
            val publicKey = activity.getPublicKey()!!
            val sign = sign("$displayName,${Json.encodeToString(postedAt)},$body", activity.getPrivateKey()!!)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sign.toByteArray())
            val md5 = MessageDigest.getInstance("md5")
            val emailDigest = md5.digest(email.toByteArray())

            return Message(type, displayName, Base64.encodeToString(publicKey.encoded, Base64.DEFAULT), emailDigest.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }, digest.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }, body, postedAt, sign, arrayListOf())
        }

        fun sign(plainText: String, privateKey: PrivateKey): String {
            val privateSignature: Signature = Signature.getInstance("SHA256withRSA")
            privateSignature.initSign(privateKey)
            privateSignature.update(plainText.toByteArray(UTF_8))
            val signature: ByteArray = privateSignature.sign()
            return Base64.encodeToString(signature, Base64.DEFAULT)
        }

        fun verify(plainText: String, signature: String, publicKey: PublicKey): Boolean {
            val publicSignature: Signature = Signature.getInstance("SHA256withRSA")
            publicSignature.initVerify(publicKey)
            publicSignature.update(plainText.toByteArray(UTF_8))
            val signatureBytes: ByteArray = Base64.decode(signature, Base64.DEFAULT)
            return publicSignature.verify(signatureBytes)
        }
    }
}