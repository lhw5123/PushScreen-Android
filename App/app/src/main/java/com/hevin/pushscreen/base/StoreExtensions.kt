package com.hevin.pushscreen.base

import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow

@MainThread
fun <S : State, A : Action> Store<S, A>.observe(
    owner: LifecycleOwner,
    observer: Observer<S>
) {
    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
        return
    }

    val subscription = observeManually(observer)

    subscription.binding = SubscriptionLifecycleBinding(owner, subscription).apply {
        owner.lifecycle.addObserver(this)
    }
}

@MainThread
fun <S : State, A : Action> Store<S, A>.observe(
    view: View,
    observer: Observer<S>
) {
    val subscription = observeManually(observer)

    subscription.binding = SubscriptionViewBinding(view, subscription).apply {
        view.addOnAttachStateChangeListener(this)
    }

    if (view.isAttachedToWindow) {
        subscription.resume()
    }
}

fun <S : State, A : Action> Store<S, A>.observeForever(observer: Observer<S>) {
    observeManually(observer).resume()
}

@ExperimentalCoroutinesApi
@MainThread
fun <S : State, A : Action> Store<S, A>.channel(
    owner: LifecycleOwner = ProcessLifecycleOwner.get()
): ReceiveChannel<S> {
    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
        throw IllegalStateException("Lifecycle is already DESTROYED")
    }

    val channel = Channel<S>(Channel.CONFLATED)

    val subscription = observeManually { state ->
        runBlocking { channel.send(state) }
    }

    subscription.binding = SubscriptionLifecycleBinding(owner, subscription).apply {
        owner.lifecycle.addObserver(this)
    }

    channel.invokeOnClose { subscription.unsubscribe() }

    return channel
}

@ExperimentalCoroutinesApi
@MainThread
fun <S : State, A : Action> Store<S, A>.flow(
    owner: LifecycleOwner? = null
): Flow<S> {
    return channelFlow {
        val subscription = observeManually { state ->
            runBlocking { send(state) }
        }

        if (owner == null) {
            subscription.resume()
        } else {
            subscription.binding = SubscriptionLifecycleBinding(owner, subscription).apply {
                owner.lifecycle.addObserver(this)
            }
        }

        awaitClose {
            subscription.unsubscribe()
        }
    }.buffer(Channel.CONFLATED)
}

@ExperimentalCoroutinesApi
@MainThread
fun <S : State, A : Action> Store<S, A>.flowScoped(
    owner: LifecycleOwner? = null,
    block: suspend (Flow<S>) -> Unit
): CoroutineScope {
    return MainScope().apply {
        launch {
            block(flow(owner))
        }
    }
}

private class SubscriptionLifecycleBinding<S : State, A : Action>(
    private val owner: LifecycleOwner,
    private val subscription: Store.Subscription<S, A>
) : LifecycleObserver, Store.Subscription.Binding {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        subscription.resume()
        Log.i("Store", "Subscription is resume.")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        subscription.pause()
        Log.i("Store", "Subscription is pause.")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        subscription.unsubscribe()
        Log.i("Store", "Subscription is unsubscribed.")
    }

    override fun unbind() {
        owner.lifecycle.removeObserver(this)
    }
}

private class SubscriptionViewBinding<S : State, A : Action>(
    private val view: View,
    private val subscription: Store.Subscription<S, A>
) : View.OnAttachStateChangeListener, Store.Subscription.Binding {

    override fun onViewAttachedToWindow(v: View?) {
        subscription.resume()
    }

    override fun onViewDetachedFromWindow(v: View?) {
        subscription.unsubscribe()
    }

    override fun unbind() {
        view.removeOnAttachStateChangeListener(this)
    }
}
