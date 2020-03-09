package com.hevin.pushscreen.state.base

import android.os.Handler
import android.os.Looper
import androidx.annotation.CheckResult
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.Executors


typealias Observer<S> = (S) -> Unit

typealias Reducer<S, A> = (S, A) -> S

/**
 * 用来存储状态。
 */
open class Store<S : State, A : Action>(
    initialState: S,
    private val reducer: Reducer<S, A>
) {
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val subscriptions = mutableSetOf<Subscription<S, A>>()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Handler(Looper.getMainLooper()).postAtFrontOfQueue {
            throw StoreException(
                "Exception while reducing state",
                throwable
            )
        }
        // 一旦出现异常，就不再接收后续的 action。因此取消这个 scope，能取消所有的 Job 并不再接收新的 action。
        scope.cancel()
    }
    private val dispatcherWithExceptionHandler = dispatcher + exceptionHandler
    private var currentState = initialState

    val state: S
        @Synchronized
        get() = currentState

    @CheckResult(suggest = "observe")
    @Synchronized
    fun observeManually(observer: Observer<S>): Subscription<S, A> {
        val subscription = Subscription(
            observer,
            store = this
        )

        synchronized(subscriptions) {
            subscriptions.add(subscription)
        }

        return subscription
    }

    fun dispatch(action: A) = scope.launch(dispatcherWithExceptionHandler) {
        dispatchInternal(action)
    }

    private fun dispatchInternal(action: A) {
        val newState = reducer(currentState, action)

        if (newState == currentState) {
            return
        }

        currentState = newState

        synchronized(subscriptions) {
            subscriptions.forEach { it.dispatch(newState) }
        }
    }

    private fun removeSubscription(subscription: Subscription<S, A>) {
        synchronized(subscription) {
            subscriptions.remove(subscription)
        }
    }

    /**
     * 会和 LifecycleOwner 或者 View 组合，响应其生命周期，从而确定是否执行 observer 函数。
     * 其中 binding 通常是 LifecycleObserver 或 View.OnAttachStateChangeListener。
     */
    class Subscription<S : State, A : Action> internal constructor(
        private val observer: Observer<S>,
        store: Store<S, A>
    ) {
        private val storeReference = WeakReference(store)
        internal var binding: Binding? = null
        private var active = false

        @Synchronized
        fun resume() {
            active = true
            storeReference.get()?.state?.let(observer)
        }

        @Synchronized
        fun pause() {
            active = false
        }

        @Synchronized
        internal fun dispatch(state: S) {
            if (active) {
                observer(state)
            }
        }

        @Synchronized
        fun unsubscribe() {
            active = false

            storeReference.get()?.removeSubscription(this)
            storeReference.clear()

            binding?.unbind()
        }

        interface Binding {
            fun unbind()
        }
    }
}

class StoreException(val msg: String, val e: Throwable? = null) : Exception(msg, e)
