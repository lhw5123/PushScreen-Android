package com.hevin.pushscreen.state

import com.hevin.pushscreen.base.Action

sealed class AppAction : Action

sealed class PushingAction : AppAction() {

    /**
     * 开始推流
     */
    data class Start(val url: String) : PushingAction()

    /**
     * 停止推流
     */
    object Stop : PushingAction()
}
