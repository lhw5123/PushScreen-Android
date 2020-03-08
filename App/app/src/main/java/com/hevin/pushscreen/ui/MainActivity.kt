package com.hevin.pushscreen.ui

import android.annotation.SuppressLint
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
import com.hevin.pushscreen.base.observe
import com.hevin.pushscreen.state.PushingAction
import kotlinx.android.synthetic.main.activity_main.*
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
            AppApplication.appStore.dispatch(PushingAction.Stop)
        }

        @SuppressLint("SetTextI18n")
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaStream = (service as MediaStream.MediaBinder).service
            mediaStream?.observePushingState(this@MainActivity,
                Observer<MediaStream.PushingState> {
                    if (it.screenPushing) {
                        AppApplication.appStore.dispatch(PushingAction.Start(url = it.url))
                    } else {
                        AppApplication.appStore.dispatch(PushingAction.Stop)
                    }
                })
        }
    }

    @SuppressLint("SetTextI18n")
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

        btnCaptureScreenStart.setOnClickListener {
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

        btnCaptureScreenStop.setOnClickListener {
            mediaStream?.stopPushScreen()
        }
    }

    private fun initView() {
        btnCaptureScreenStart.isEnabled = !AppApplication.appStore.state.isPushingScreen
        btnCaptureScreenStop.isEnabled = AppApplication.appStore.state.isPushingScreen
    }

    @SuppressLint("SetTextI18n")
    private fun observeAppState() {
        AppApplication.appStore.observe(this) {
            runOnUiThread {
                tvUrl.text = if (it.isPushingScreen) {
                    it.pushingUrl
                } else {
                    ""
                }
                btnCaptureScreenStart.isEnabled = !it.isPushingScreen
                btnCaptureScreenStop.isEnabled = it.isPushingScreen
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null && mediaStream != null) {
                // 获得了录屏权限
                mediaStream!!.pushScreen(resultCode, data, HOST, PORT, "stream1")
            }
        }
    }
}
