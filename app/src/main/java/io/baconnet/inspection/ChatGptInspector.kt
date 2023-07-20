package io.baconnet.inspection

import android.R.id.message
import android.content.Context
import android.util.Log
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import io.baconnet.MainActivity
import java.lang.Exception
import java.util.List


class ChatGptInspector : Inspector {
    override fun inspect(context: Context, displayName: String, text: String, callback: (result: Boolean) -> Unit): Unit {
        if ((context as MainActivity).getOpenAIKey() == "") {
            callback(false)
        } else {
            try {
                Log.i("Inspector", "Inspecting: $displayName $text")
                val service = OpenAiService(context.getOpenAIKey())
                val chatCompletionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(listOf(ChatMessage(ChatMessageRole.SYSTEM.value(), "次に挙げる文章の中に不適切な表現が入っているなら「reject」、適切なら「pass」を出力してください。不適切というのは、公序良俗に違反するものを意味しています。他者に対する攻撃的な発言や、卑猥な表現なども含みます。"),
                        ChatMessage(ChatMessageRole.USER.value(), "投稿者名: $displayName\n投稿本文: $text")))
                    .build()

                val response: String =
                    service.createChatCompletion(chatCompletionRequest).choices[0].message
                        .content

                if (response.contains(Regex("pass"))) {
                    callback(true)
                } else if (response.contains(Regex("reject"))) {
                    callback(false)
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }
}