package com.hevin.pushscreen.state

internal object AppReducer {
    fun reducer(currentState: AppState, action: AppAction): AppState {
        return when (action) {
            is PushingAction.Start -> {
                val updatedStreamState = currentState.streamState.copy(url = action.url, isPushing = true)
                currentState.copy(streamState = updatedStreamState)
            }
            is PushingAction.Stop -> {
                val updatedStreamState = currentState.streamState.copy(
                    url = action.url,
                    isPushing = false,
                    errorMessage = action.message
                )
                currentState.copy(streamState = updatedStreamState)
            }
            else -> currentState
        }
    }
}
