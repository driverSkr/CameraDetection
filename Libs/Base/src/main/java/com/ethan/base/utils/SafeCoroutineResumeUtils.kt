package com.ethan.base.utils

import kotlin.coroutines.Continuation

fun <T> Continuation<T>.resumeWithOutError(value: T): Unit {
    try {
        resumeWith(Result.success(value))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
