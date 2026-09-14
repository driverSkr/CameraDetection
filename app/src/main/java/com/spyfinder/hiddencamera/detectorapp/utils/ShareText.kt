package com.spyfinder.hiddencamera.detectorapp.utils

object ShareText {
    fun body(text: String, url: String): String = if (url in text) text else "${text.trim()}\n$url"
}
