package io.baconnet.nmst

import java.util.Date

enum class SubscribeErrorCode {

}

enum class SendErrorCode {}

class NmstClient {
    companion object {
        const val VERSION = 0
    }
    // 失敗したら例外を投げる
    fun init(): Unit {}
    fun subscribe(onReceive: (msg: Message) -> Unit, onError: (code: SubscribeErrorCode) -> Unit): Unit {
        onReceive(Message("名無し", "162ea6d23c3df5b4379c636c491e08883f547da86d0c23165bf2a8fdd9a25514", "c0e89a293bd36c7a768e4e9d2c5475a8", "こんにちは", Date()))
    }
    fun send(msg: Message, onSend: () -> Unit, onError: (code: SendErrorCode) -> Unit): Unit {
        onSend()
    }
}