package com.nakwonelec.wallpaper.shinycolors

import android.app.Presentation
import android.content.*
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import java.util.*

class MyWallpaperService: WallpaperService() {
    companion object {
        // We can have multiple engines running at once (since you might have one on your home screen
        // and another in the settings panel, for instance), so for debugging it's useful to keep track
        // of which one is which. We give each an id based on this nextEngineId.
        var nextEngineId = 1
    }

    lateinit var sps: SharedPreferences


    override fun onCreate() {
        super.onCreate()
        sps = PreferenceManager.getDefaultSharedPreferences(this)
    }
    override fun onCreateEngine(): Engine {
        return MyEngine(this)
    }

    inner class MyEngine(context: Context): Engine() {
        val myId = nextEngineId++
        val myContext: Context = context
        var myWebView: WebView? = null
        var myWebViewClient: MyWebViewClient? = null
        var myHolder: SurfaceHolder? = null
        var myPresentation: Presentation? = null
        var myVirtualDisplay: VirtualDisplay? = null
        var myMessageReceiver: MyBroadcastReceiver? = null
        val myDisplayListener: MyDisplayListener = MyDisplayListener()
        var IsOnSurfaceChangedExecuted = false
        var onVisivilityTimer: Timer? = null
        val myGestureDetector = GestureDetector(myContext, MySimpleOnGestureListener())
        var orientation = Configuration.ORIENTATION_PORTRAIT
        var IsDelayVisible = false

        private fun log(message: String) {
            Log.d("MyWPS $myId", message)
        }

        private fun logError(message: String) {
            Log.e("MyWPS $myId", message)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            log("onCreate")
            super.onCreate(surfaceHolder)
        }

        override fun onDestroy() {
            log("onDestroy")
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            log("onSurfaceCreated")
            super.onSurfaceCreated(holder)
            holder?.let {
                myHolder = it
                myMessageReceiver = myMessageReceiver?.run {
                    LocalBroadcastManager.getInstance(myContext).unregisterReceiver(this)
                    null
                }
                myWebView?.run{destroy(); null}
                myPresentation?.run{dismiss(); null}
                myVirtualDisplay?.run { release(); null }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            log("onSurfaceDestroyed")
            myHolder = null
            myMessageReceiver = myMessageReceiver?.run {
                LocalBroadcastManager.getInstance(myContext).unregisterReceiver(this)
                null
            }
            (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).let {
                it.unregisterDisplayListener(myDisplayListener)
            }
            myWebView = myWebView?.run{destroy(); null}
            myPresentation = myPresentation?.run{dismiss(); null}
            myVirtualDisplay = myVirtualDisplay?.run { release(); null }
            super.onSurfaceDestroyed(holder)

        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            log("onSurfaceChanged(holder, $format, $width, $height")
            super.onSurfaceChanged(holder, format, width, height)

            val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            val density = DisplayMetrics.DENSITY_DEFAULT
            if(myWebView != null && myVirtualDisplay != null && myPresentation != null) {
                log("onSurfaceChanged: rotation")
                myVirtualDisplay!!.resize(width,height,density)
                when {
                    width>height -> {
                        changeWebviewLayout(
                            myWebView!!,
                            Configuration.ORIENTATION_LANDSCAPE,
                            myHolder!!
                        )
                        orientation = Configuration.ORIENTATION_LANDSCAPE
                    }
                    else -> {
                        changeWebviewLayout(
                            myWebView!!,
                            Configuration.ORIENTATION_PORTRAIT,
                            myHolder!!,
                        )
                        orientation = Configuration.ORIENTATION_PORTRAIT
                    }
                }
                IsOnSurfaceChangedExecuted = true
            } else {
                myWebView = myWebView?.run{destroy(); null}
                myPresentation = myPresentation?.run{dismiss(); null}
                myVirtualDisplay = myVirtualDisplay?.run { release(); null }

                myHolder = holder
                val mDisplayManager =
                    getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                myVirtualDisplay = mDisplayManager.createVirtualDisplay(
                    "MyVirtualDisplay",
                    width, height, density, holder.surface, flags
                )
                myPresentation = object: Presentation(myContext, myVirtualDisplay!!.getDisplay()) {
                    override fun cancel() { }
                    override fun onDisplayChanged() { }
                }
                myPresentation!!.setContentView(R.layout.presentation_webview)
                WebView.setWebContentsDebuggingEnabled(true)
                myWebView = myPresentation!!.findViewById<WebView>(R.id.webview_presentation)!!.also {
                    myWebViewClient = MyWebViewClient().apply {
                        sound = sps.getBoolean(Settings.Boolean.Sound.toString(), false)
                    }
                    it.webViewClient = myWebViewClient as MyWebViewClient
                    it.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        textZoom = 100
                        mediaPlaybackRequiresUserGesture = true
                        userAgentString = getString(R.string.desktop_ua)
                    }
                    it.loadUrl(getString(R.string.shinyolors_home_url))
                }
                myMessageReceiver = MyBroadcastReceiver().also {
                    LocalBroadcastManager.getInstance(myContext).registerReceiver(it, IntentFilter(intent.Settings.toString()))
                }
                mDisplayManager.registerDisplayListener(myDisplayListener, null)
                orientation = myContext.resources.configuration.orientation
                myPresentation!!.show()

                delayedNotifyColorsChanged(30000)
            }
        }

        fun delayedNotifyColorsChanged(delay: Long) {
            Timer().schedule(object: TimerTask() {
                @RequiresApi(Build.VERSION_CODES.O_MR1)
                override fun run() {
                    notifyColorsChanged()
                }
            }, delay)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            log("onVisibilityChanged($visible")
            super.onVisibilityChanged(visible)
            if(visible == true) {
                onVisivilityTimer = onVisivilityTimer?.run { cancel(); null }
                onVisivilityTimer = Timer().apply{
                    schedule(object:TimerTask(){
                        override fun run() {
                            IsDelayVisible = true
                            if(isVisible) myWebView?.post {
                                myWebView!!.onResume()
                            }
                        }
                    }, 500)
                }
            } else {
                onVisivilityTimer = onVisivilityTimer?.run { cancel(); null }
                IsDelayVisible = false
                myWebView?.onPause()
            }
            /// previous code
//            if (myWebView != null) {
//                if(visible == false) {
//                    myWebView?.onPause()
//                } else {
//                    myWebView?.onResume()
//                }
//            }
        }

        var lastmultitouch = 0L
        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            log("onTouchEvent(x: ${event.x} y: ${event.y} rawx: ${event.rawX} rawy: ${event.rawY} pointerCount: ${event.pointerCount} action: ${event.action}")
            if(IsDelayVisible) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> log("onTouchEvent: ACTION_DOWN")
                    MotionEvent.ACTION_UP -> log("onTouchEvent: ACTION_UP")
                    MotionEvent.ACTION_BUTTON_PRESS -> log("onTouchEvent: ACTION_BUTTON_PRESS")
                    MotionEvent.ACTION_BUTTON_RELEASE -> log("onTouchEvent: ACTION_BUTTON_RELEASE")
                }
                myGestureDetector.onTouchEvent(event)

                if (event.pointerCount > 1 && event.action == MotionEvent.ACTION_POINTER_UP) {
                    log("onTouchEvent: multitouch(${event.pointerCount}) event")
                    if (event.eventTime - lastmultitouch < 1500) {
                        log("onTouchEvent: reload")
                        val intent = Intent(intent.Settings.toString())
                        intent.putExtra(
                            Settings.Control.toString(),
                            Settings.Control.Back.toString()
                        )
                        myContext?.let {
                            LocalBroadcastManager.getInstance(it).sendBroadcast(intent)
                        }
                    }
                    lastmultitouch = event.eventTime
                }
            }
        }

//        @RequiresApi(Build.VERSION_CODES.O_MR1)
//        override fun onComputeColors(): WallpaperColors? {
//
//            myWebView?.let {
//                if (it.width != 0 && it.height != 0) {
//                    val bitmap = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
//
//                    val canvas = Canvas(bitmap)
//                    it.draw(canvas)
//                    var cropBitmap: Bitmap
//                    if (myWebView!!.width / 2
//                        - myHolder!!.surfaceFrame.right / 2 < 0
//                    ) {
//                        cropBitmap = bitmap
//                    } else {
//                        cropBitmap = Bitmap.createBitmap(
//                            bitmap,
//                            myWebView!!.width / 2
//                                    - myHolder!!.surfaceFrame.right / 2,
//                            0,
//                            myHolder!!.surfaceFrame.right,
//                            myHolder!!.surfaceFrame.bottom
//                        )
//                    }
//
//                    val storage = cacheDir
//                    val fileName = "test.jpg"
//                    val tempFile = File(storage, fileName)
//                    tempFile.createNewFile()
//                    val out = FileOutputStream(tempFile)
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
//                    out.close()
//
//                    val wallpaperColors = WallpaperColors.fromBitmap(cropBitmap)
//                    return wallpaperColors
//                }
//            }
//            return super.onComputeColors()
//        }

//        @RequiresApi(Build.VERSION_CODES.O_MR1)
//        override fun onComputeColors(): WallpaperColors? {
//            myWebView?.let {
//                val bitmap = BitmapFactory.decodeStream(resources.assets.open("sample.png"))
//                val wallpaperColors = WallpaperColors.fromBitmap(bitmap)
//                return wallpaperColors
//            }
//            return super.onComputeColors()
//        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
            log("onOffsetsChanged(${xOffset}, ${yOffset}, ${xOffsetStep}, ${yOffsetStep}, ${xPixelOffset}, ${yPixelOffset}")
        }

        inner class MyWebViewClient(): WebViewClient() {
            public var sound = false

//            override fun onPageStarted(
//                view: WebView?,
//                url: String?,
//                favicon: Bitmap?
//            ) {
//                super.onPageStarted(view, url, favicon)
//                val open = this@MyEngine.myContext.resources.assets.open("js/ShinyColors.user.js")
//                var inputStreamReader = open.bufferedReader(Charsets.UTF_8)
//                var script = inputStreamReader.readText()
//                view?.evaluateJavascript("javascript:"+ script, { value -> })
//            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/7f697481939646e7371fd37596e0055b265b229c4f94dffac38bf51b87fce6a4")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("7f697481939646e7371fd37596e0055b265b229c4f94dffac38bf51b87fce6a4.png"))
                }
                if(request?.url.toString().contains("newbiepr.github.io/Temporary_KRTL/data/image/my_page.parts.png")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("7f697481939646e7371fd37596e0055b265b229c4f94dffac38bf51b87fce6a4.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/339f113f638b026327dc3b5f678083c1cacf49073f2fa85574f4ae9c7dde8fbb")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("339f113f638b026327dc3b5f678083c1cacf49073f2fa85574f4ae9c7dde8fbb.png"))
                }
                /// 홈 화면 말풍선
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/34b62a9e7c7dbc6a65236863c97846b0cadc7a27ca8cd2ed0ff5f5177291f40c")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("34b62a9e7c7dbc6a65236863c97846b0cadc7a27ca8cd2ed0ff5f5177291f40c.png"))
                }
                /// 왼쪽 가운데 메뉴버튼, 행사 더보기 버튼
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/8f5a4652a6d1d7a160fa53c7c06f245463066358eaf592a4be18e5a7a634f24a")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("8f5a4652a6d1d7a160fa53c7c06f245463066358eaf592a4be18e5a7a634f24a.png"))
                }
                /// 페스 버튼
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/838bc741063799df09eef6f032f97704eb6b8932ec45ea53de3690da5b54a721")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("838bc741063799df09eef6f032f97704eb6b8932ec45ea53de3690da5b54a721.png"))
                }
                /// 2.5기념버튼
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/6a93d1e1277ff64d8e387d0970ee6679575db722c66c069edc1fa017ef9362f1")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("6a93d1e1277ff64d8e387d0970ee6679575db722c66c069edc1fa017ef9362f1.png"))
                }
                /// 왼쪽 이벤트 배경들
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/2ea5cb08f06198ab5f08750221c72b8defb88ba661e61002d510322eabf44495")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("2ea5cb08f06198ab5f08750221c72b8defb88ba661e61002d510322eabf44495.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/77b0bc65a8722449de6077d0103d9cbca7ad0813cd2679d7bb19a35e6f8f7ce3")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("77b0bc65a8722449de6077d0103d9cbca7ad0813cd2679d7bb19a35e6f8f7ce3.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/2f7aa4ddb3a34dcc29e00535626fd82ca9d149655883bb5a5a3eac7bee2e6970")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("2f7aa4ddb3a34dcc29e00535626fd82ca9d149655883bb5a5a3eac7bee2e6970.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/82b78308f00889cc2fdfae612fe1b5bdb0a6a14e1aa586833e137b525eae0e1f")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("82b78308f00889cc2fdfae612fe1b5bdb0a6a14e1aa586833e137b525eae0e1f.png"))
                }
                /// 왼쪽 오른쪽 이벤트 버튼
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/16da4627eb39a4cfc9564f8be263a12a810746dd5ca2e8de4bd19cdfa6d96969")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("16da4627eb39a4cfc9564f8be263a12a810746dd5ca2e8de4bd19cdfa6d96969.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/a5ffae3dda6990f5a88ed22886399b4c31afa7d45759b90ecdc979c90add7b9c")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("a5ffae3dda6990f5a88ed22886399b4c31afa7d45759b90ecdc979c90add7b9c.png"))
                } ///
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/e92d50a2cfca8c57f9847e521f4ff5da788fbc15de593d0022c755ce63c01f0d")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("e92d50a2cfca8c57f9847e521f4ff5da788fbc15de593d0022c755ce63c01f0d.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/b223090f115e90342407c2adbc74915a1fc0316a5dd2abc59afe220fb76b55cf")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("b223090f115e90342407c2adbc74915a1fc0316a5dd2abc59afe220fb76b55cf.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/5908652bb24ee55b02b32180bf6d169ea31193f421f733f1ed4602da601a11c5")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("5908652bb24ee55b02b32180bf6d169ea31193f421f733f1ed4602da601a11c5.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/f5d2aeb7ed213dd2a1b9440d70ecf584b8f2dc4ef0bb8715580962ea01de3255")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("f5d2aeb7ed213dd2a1b9440d70ecf584b8f2dc4ef0bb8715580962ea01de3255.png"))
                } ///
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/e717d7f33398fa83426d14f60aac9229a9f5f7443122769f7dc5df744098e553")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("e717d7f33398fa83426d14f60aac9229a9f5f7443122769f7dc5df744098e553.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/4dbd41dbc80a312a8491dd9504370f892b74ce755cccef41d80dee92c1b23c2e")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("4dbd41dbc80a312a8491dd9504370f892b74ce755cccef41d80dee92c1b23c2e.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/434c4ab4df5d1ed0c6dda9889d705612b7f689a4929833bbc0b64e91e0f562a7")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("434c4ab4df5d1ed0c6dda9889d705612b7f689a4929833bbc0b64e91e0f562a7.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/bd5cda744ff82aee0bf54078283601f925fb927cecfd122696594de5009d8cc0")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("bd5cda744ff82aee0bf54078283601f925fb927cecfd122696594de5009d8cc0.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/aef290419a47d1c1ad57ef92a484ad7b344cda12a7720368382d9b44fe551aff")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("aef290419a47d1c1ad57ef92a484ad7b344cda12a7720368382d9b44fe551aff.png"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/74c484bca9b827a900a262ab1f20cf37b6ce40fa2274ea459bb8258844766316")) {
                    return WebResourceResponse("image/png","",this@MyEngine.myContext.resources.assets.open("74c484bca9b827a900a262ab1f20cf37b6ce40fa2274ea459bb8258844766316.png"))
                }
                /// 폰트
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/fonts/primula-HummingStd-E.woff2")) {
                    return WebResourceResponse("font/woff2",null,this@MyEngine.myContext.resources.assets.open("primula-HummingStd-E.woff2"))
                }
                if(request?.url.toString().contains("shinycolors.enza.fun/assets/fonts/primula-UDKakugo_SmallPr6-B.woff2")) {
                    return WebResourceResponse("font/woff2",null,this@MyEngine.myContext.resources.assets.open("primula-UDKakugo_SmallPr6-B.woff2"))
                }
                if(request?.url.toString().contains("newbiepr.github.io/Temporary_KRTL/data/font/heiti.woff2")) {
                    return WebResourceResponse("font/woff2",null,this@MyEngine.myContext.resources.assets.open("heiti.woff2"))
                }
                if(!sound && request?.url.toString().contains(""".*\.m4a.*""".toRegex())) {
                    return WebResourceResponse("","",null)
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        inner class MyBroadcastReceiver: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.getStringExtra(Settings.Control.toString())?.let { command ->
                    when(Settings.Control.valueOf(command)) {
                        Settings.Control.Reload -> {
                            log("Command.Reload")
                            myWebView?.let {
                                it.loadUrl(getString(R.string.shinyolors_home_url))
                                it.reload()
                                delayedNotifyColorsChanged(20000)
                            }
                        }
                        Settings.Control.Back -> {
                            myWebView?.let {
                                log("Command.Back")
                                clickBackButton(it,1000)
                                clickCenter1Button(it, 800)
                                clickBackButton(it,600)
                                clickCenter1Button(it, 400)
                                clickBackButton(it,200)


                            }
                        }
                    }
                }
                intent?.getStringExtra(Settings.Boolean.toString())?.let { b ->
                    when(Settings.Boolean.valueOf(b)) {
                        Settings.Boolean.Sound -> {
                            log("Boolean.Sound")
                            sps?.getBoolean(Settings.Boolean.Sound.toString(), false)?.let {
                                b: Boolean ->
                                myWebViewClient?.sound = b
                                myWebView?.reload()
                            }
                        }
                    }
                }
            }

            fun clickBackButton(it: WebView, delay: Long = 0) {
                val motionEvent3 = MotionEvent.obtain(
                    SystemClock.uptimeMillis()-delay-50,
                    SystemClock.uptimeMillis()-delay-50,
                    MotionEvent.ACTION_DOWN,
                    it.width*50.0f/1136.0f,
                    it.height*589.0f/640.0f,
                    0)
                it.onTouchEvent(motionEvent3)
                val motionEvent4 = MotionEvent.obtain(
                    SystemClock.uptimeMillis()-delay,
                    SystemClock.uptimeMillis()-delay,
                    MotionEvent.ACTION_UP,
                    it.width*50.0f/1136.0f,
                    it.height*589.0f/640.0f,
                    0)
                it.onTouchEvent(motionEvent4)
            }

            fun clickCenter1Button(it: WebView, delay: Long = 0) {

                val motionEvent = MotionEvent.obtain(
                    SystemClock.uptimeMillis()-delay-50,
                    SystemClock.uptimeMillis()-delay-50,
                    MotionEvent.ACTION_DOWN,
                    it.width*528.0f/1136.0f,
                    it.height*555.0f/640.0f,
                    0)
                it.onTouchEvent(motionEvent)
                val motionEvent2 = MotionEvent.obtain(
                    SystemClock.uptimeMillis()-delay,
                    SystemClock.uptimeMillis()-delay,
                    MotionEvent.ACTION_UP,
                    it.width*528.0f/1136.0f,
                    it.height*555.0f/640.0f,
                    0)
                it.onTouchEvent(motionEvent2)
            }

            fun clickCenter2Button(it: WebView, delay: Long = 0) {
                val motionEvent3 = MotionEvent.obtain(
                    SystemClock.uptimeMillis()-delay-50,
                    SystemClock.uptimeMillis()-delay-50,
                    MotionEvent.ACTION_DOWN,
                    it.width*537.0f/1136.0f,
                    it.height*511.0f/640.0f,
                    0)
                it.onTouchEvent(motionEvent3)
                val motionEvent4 = MotionEvent.obtain(
                    SystemClock.uptimeMillis()-delay,
                    SystemClock.uptimeMillis()-delay,
                    MotionEvent.ACTION_UP,
                    it.width*537.0f/1136.0f,
                    it.height*511.0f/640.0f,
                    0)
                it.onTouchEvent(motionEvent4)
            }
        }

        /// http://andraskindler.com/blog/2015/live-wallpaper-onoffsetchanged-scrolling/
        inner class MySimpleOnGestureListener(): GestureDetector.SimpleOnGestureListener() {
            var xOffset = 0.5f
            val numberOfPages = 3
            /// TODO move to engine
            var isOnOffsetsChangedWorking = true
            var doubleTapEnabled = true
            var infiniteScrollingEnabled = false

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                log("onScroll((${e1.x}, ${e1.y}, ${e1.action}), (${e2.x}, ${e2.y}, ${e2.action}), ${distanceX}, ${distanceY})")

                this@MyEngine.myHolder?.let {
                    val newXOffset = xOffset + distanceX / it.surfaceFrame.width() / numberOfPages
                    xOffset = newXOffset
                    if(!infiniteScrollingEnabled) {
                        if( newXOffset > 1 ) {
                            xOffset = 1f
                        } else if ( newXOffset < 0 ) {
                            xOffset = 0f
                        }
                    }
                }

                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                log("onFling((${e1.x}, ${e1.y}, ${e1.action}), (${e2.x}, ${e2.y}, ${e2.action}), ${velocityX}, ${velocityY})")

                var endValue = when {
                    velocityX > 0 -> (xOffset - (xOffset % (1f / numberOfPages)))
                    else -> (xOffset - (xOffset % (1f / numberOfPages)) + (1 / numberOfPages))
                }
                if(!infiniteScrollingEnabled) {
                    if(endValue<0f) {
                        endValue = 0f
                    } else if (endValue>1f) {
                        endValue = 1f
                    }
                    log("xOffset: ${xOffset}, endValue: ${endValue}")
                }

                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                log("onSingleTapUp((${e.x}, ${e.y}, ${e.action})")
                if (myWebView != null && myHolder != null) {
                    if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                        e.setLocation(
                            e.x
                                    + myWebView!!.width/2
                                    - myHolder!!.surfaceFrame.right/2,
                            e.y)

                    } else {
                        e.setLocation(
                            e.x,
                            e.y
                                + myWebView!!.height/2
                                - myHolder!!.surfaceFrame.height()/2
                        )
                    }
                    log("new x: " + e.x + " y: " + e.y)
                    if(determineValidArea(myWebView!!.width, myWebView!!.height, e)) {
                        e.action = MotionEvent.ACTION_DOWN
                        myWebView!!.onTouchEvent(e)
                        e.action = MotionEvent.ACTION_UP
                        myWebView!!.onTouchEvent(e)
                    }
                }
                return super.onSingleTapUp(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                log("onSingleTapConfirmed((${e.x}, ${e.y}, ${e.action})")
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                log("onDoubleTap((${e.x}, ${e.y}, ${e.action})")
                return super.onDoubleTap(e)
            }

            fun determineValidArea(webviewWidth: Int, webviewHeight: Int, e: MotionEvent): Boolean {
                val WIDTH = 1136.0f
                val HEIGHT = 640f
                if(e.x < webviewWidth*(315f)/WIDTH &&
                    e.y > webviewHeight*500.0f/HEIGHT) {
                    return false
                }
                if(e.x > webviewWidth*325.0f/WIDTH &&
                    e.y > webviewHeight*515.0f/HEIGHT) {
                    return false
                }
                if(e.x > webviewWidth*(810f)/WIDTH &&
                    e.y > webviewHeight*480.0f/HEIGHT) {
                    return false
                }
                if(e.x > webviewWidth*(885f)/WIDTH &&
                    e.y > webviewHeight*410.0f/HEIGHT) {
                    return false
                }
                if(e.x > webviewWidth*(790f)/WIDTH &&
                    e.y < webviewHeight*75.0f/HEIGHT) {
                    return false
                }
                if(e.y < webviewHeight*55.0f/HEIGHT) {
                    return false
                }
                if(e.x < webviewWidth*(220f)/WIDTH &&
                    e.y < webviewHeight*150.0f/HEIGHT) {
                    return false
                }
                if(e.x < webviewWidth*(80f)/WIDTH &&
                    e.y < webviewHeight*370.0f/HEIGHT) {
                    return false
                }
                return true
            }
        }

        /// https://stackoverflow.com/questions/9909037/how-to-detect-screen-rotation-through-180-degrees-from-landscape-to-landscape-or
        /// https://jamssoft.tistory.com/104
        inner class MyDisplayListener(): DisplayManager.DisplayListener {
            val display = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

            override fun onDisplayAdded(displayId: Int) {
                log("Display #${displayId} added")
            }

            override fun onDisplayRemoved(displayId: Int) {
                log("Display #${displayId} removed")
            }

            override fun onDisplayChanged(displayId: Int) {
                log("Display #${displayId} changed")
                if(!IsOnSurfaceChangedExecuted) {
                    val angle = when {
                        display.displays.isNotEmpty() -> {
                            when (display.displays[0].rotation) {
                                Surface.ROTATION_0 -> 0
                                Surface.ROTATION_90 -> 270
                                Surface.ROTATION_180 -> 180
                                Surface.ROTATION_270 -> 90
                                else -> 0
                            }
                        }
                        else -> 0
                    }
                    val config_orientation = when(angle) {
                        0, 180, 360 -> Configuration.ORIENTATION_PORTRAIT
                        90, 270 -> Configuration.ORIENTATION_LANDSCAPE
                        else -> Configuration.ORIENTATION_UNDEFINED
                    }
                    this@MyEngine.orientation = config_orientation
                    log("onDisplayChanged(): orientation: ${config_orientation} run instead of onSurfaceChanged")
                    myWebView?.let {
                        it.post {
                            when (angle) {
                                0, 360 -> it.rotation = 0.0f
                                90 -> it.rotation =270.0f
                                180 -> it.rotation =180.0f
                                270 -> it.rotation =90.0f
                            }
                            changeWebviewLayout(it, myContext.resources.configuration.orientation, myHolder!!, true)
                        }
                    }
                }
            }
        }

        fun changeWebviewLayout(webview: WebView, orientation: Int, holder: SurfaceHolder, bugfix: Boolean = false) {
            when(orientation) {
                Configuration.ORIENTATION_PORTRAIT,
                Configuration.ORIENTATION_UNDEFINED -> {
                    webview.layoutParams   = (webview.layoutParams as ConstraintLayout.LayoutParams).apply {
                        width =
                            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0F, resources.displayMetrics)
                                .toInt()
                        height = ConstraintLayout.LayoutParams.MATCH_PARENT
                        dimensionRatio = "16:9"
                    }
                }
                Configuration.ORIENTATION_LANDSCAPE -> {
                    webview.layoutParams   = (webview.layoutParams as ConstraintLayout.LayoutParams).apply {
                        if(bugfix) {
                            width = holder.surfaceFrame.height()
                            height = holder.surfaceFrame.height()*16/9
                        } else {
                            width = holder.surfaceFrame.width()
                            height = holder.surfaceFrame.width()*16/9
                        }
                        dimensionRatio = null
                    }
                }
            }
        }
    }
}