package io.baconnet.inspection

import android.content.Context

interface Inspector {
    fun inspect(context: Context, displayName: String, text: String, callback: (result: Boolean) -> Unit): Unit;
}