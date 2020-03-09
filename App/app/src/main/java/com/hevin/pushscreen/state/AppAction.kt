package com.hevin.pushscreen.state

import com.hevin.pushscreen.state.base.Action

sealed class AppAction : Action

sealed class PushingAction : AppAction() {

    /**
     * 开始推流
     *
     * @param url 推流地址
     */
    data class Start(val url: String) : PushingAction()

    /**
     * 停止推流
     *
     * @param message 错误信息
     */
    data class Stop(val url: String, val message: String = "") : PushingAction()
}
