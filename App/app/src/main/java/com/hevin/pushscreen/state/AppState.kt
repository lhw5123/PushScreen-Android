package com.hevin.pushscreen.state

import com.hevin.pushscreen.state.base.State

/**
 * 记录应用状态。
 */
data class AppState(
    val streamState: StreamState = StreamState()
) : State

/**
 * 推流状态
 */
data class StreamState(
    /**
     * 推流的 URL
     */
    val url: String = "",

    /**
     * 是否正在推流
     */
    val isPushing: Boolean = false,

    /**
     * 导致停止推流的错误的信息
     */
    val errorMessage: String = ""
) : State
