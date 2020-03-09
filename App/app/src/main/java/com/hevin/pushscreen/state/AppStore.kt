package com.hevin.pushscreen.state

import com.hevin.pushscreen.state.base.Store

class AppStore(
    initialState: AppState = AppState()
) : Store<AppState, AppAction>(
    initialState, AppReducer::reducer
)