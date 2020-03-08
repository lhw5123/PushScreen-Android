package com.hevin.pushscreen.state

internal object AppReducer {
    fun reducer(currentState: AppState, action: AppAction): AppState {
        return when (action) {
            is PushingAction.Start -> {
                currentState.copy(isPushingScreen = true, pushingUrl = action.url)
            }
            is PushingAction.Stop -> {
                currentState.copy(isPushingScreen = false)
            }
            else -> currentState
        }
    }
}
