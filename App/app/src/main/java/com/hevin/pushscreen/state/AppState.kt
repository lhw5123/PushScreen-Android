package com.hevin.pushscreen.state

import com.hevin.pushscreen.base.State

/**
 * 记录应用状态。
 */
data class AppState(
    /**
     * 是否正在推流
     */
    val isPushingScreen: Boolean = false,
    /**
     * 推流的 URL
     */
    val pushingUrl: String = ""
) : State
