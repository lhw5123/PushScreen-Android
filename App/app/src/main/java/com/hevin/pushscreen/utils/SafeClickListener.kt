package com.hevin.pushscreen.utils

import android.os.SystemClock
import android.view.View

class SafeClickListener(
    private var defaultInternal: Int = 1000,
    private val onSafeClick: (View) -> Unit
) : View.OnClickListener {
    private var lastTimeClicked: Long = 0

    override fun onClick(v: View) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastTimeClicked < defaultInternal) {
            return
        }
        lastTimeClicked = currentTime
        onSafeClick(v)
    }
}

fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
    val safeClickListener = SafeClickListener {
        onSafeClick(it)
    }
    setOnClickListener(safeClickListener)
}
