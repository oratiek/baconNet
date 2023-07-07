package io.baconnet.inspection

interface Inspector {
    fun inspect(displayName: String, text: String, callback: (result: Boolean) -> Unit): Unit;
}