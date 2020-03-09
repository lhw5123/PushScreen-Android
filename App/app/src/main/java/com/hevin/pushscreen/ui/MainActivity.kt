package com.hevin.pushscreen.ui

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.hevin.pushscreen.AppApplication
import com.hevin.pushscreen.R
import com.hevin.pushscreen.state.PushingAction
import com.hevin.pushscreen.state.base.observe
import com.hevin.pushscreen.utils.setSafeOnClickListener
import kotlinx.android.synthetic.main.activity_main.*
import org.easydarwin.push.EasyPusher.OnInitPusherCallback.CODE.*
import org.easydarwin.push.MediaStream

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_MEDIA_PROJECTION = 1000
        private const val HOST = "cloud.easydarwin.org"
        /**
         * RTSP 的端口号
         */
        private const val PORT = "554"
    }

    private var mediaStream: MediaStream? = null

    private val mediaStreamConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mediaStream = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaStream = (service as MediaStream.MediaBinder).service
            mediaStream?.observePushingState(this@MainActivity,
                Observer<MediaStream.PushingState> {
                    Log.d(TAG, "MediaStream pushing state: ${it.state}")
                    // Note: 因为 MediaStream 返回的和 EasyPusher 中定义的状态不完全一致，因此存在一些问题。
                    // 开始推流之后，MediaStream 会不断返回状态 1、2、5，因此 dispatch action 给 store 统一处理，可以过滤掉重复状态。
                    // 调用 MediaStream 的 stop 方法后，返回的状态是硬编码 0，因此下面的停止推流判断加上 0。
                    when (it.state) {
                        EASY_PUSH_STATE_CONNECTED, EASY_PUSH_STATE_PUSHING -> {
                            // 开始推流
                            AppApplication.appStore.dispatch(PushingAction.Start(url = it.url))
                        }
                        0, EASY_PUSH_STATE_DISCONNECTED, EASY_PUSH_STATE_CONNECT_ABORT -> {
                            // 停止推流
                            AppApplication.appStore.dispatch(PushingAction.Stop(url = it.url, message = it.msg))
                        }
                    }
                })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        observeAppState()

        bindService(
            Intent(this, MediaStream::class.java),
            mediaStreamConnection,
            Context.BIND_AUTO_CREATE
        )

        btnCaptureScreenStart.setSafeOnClickListener {
            // 去请求录屏权限
            val projectManager =
                applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = projectManager.createScreenCaptureIntent()
            // 避免少数机型上可能没有授权的 Activity，而导致崩溃。
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                try {
                    startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
                } catch (e: ActivityNotFoundException) {
                    Log.d(TAG, "Don't found Activity for ScreenCaptureIntent.")
                }
            }
        }

        btnCaptureScreenStop.setSafeOnClickListener {
            mediaStream?.stopPushScreen()
        }
    }

    private fun initView() {
        btnCaptureScreenStart.isEnabled = !AppApplication.appStore.state.streamState.isPushing
        btnCaptureScreenStop.isEnabled = AppApplication.appStore.state.streamState.isPushing
    }

    private fun observeAppState() {
        AppApplication.appStore.observe(this) {
            runOnUiThread {
                if (it.streamState.isPushing) {
                    tvMessage.text = getString(R.string.pushingScreen_url, it.streamState.url)
                    btnCaptureScreenStart.isEnabled = false
                    btnCaptureScreenStop.isEnabled = true
                } else {
                    tvMessage.text = it.streamState.errorMessage
                    btnCaptureScreenStart.isEnabled = true
                    btnCaptureScreenStop.isEnabled = false
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 获得了录屏权限
                if (mediaStream == null) {
                    throw Throwable("MediaStream don't init.")
                } else {
                    mediaStream!!.pushScreen(resultCode, data, HOST, PORT, "stream1")
                }
            }
        }
    }
}
