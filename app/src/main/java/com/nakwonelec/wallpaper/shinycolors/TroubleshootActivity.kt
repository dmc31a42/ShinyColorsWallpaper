package com.nakwonelec.wallpaper.shinycolors

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ScrollView
import kotlinx.android.synthetic.main.activity_troubleshoot.*
import org.w3c.dom.Document

class TroubleshootActivity : DocumentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_troubleshoot)

        textView__2.setOnClickListener {
            scrollToView(textView_1, scrollView)
        }
        textView__3.setOnClickListener {
            scrollToView(textView1, scrollView)
        }
        textView__4.setOnClickListener {
            scrollToView(textView8, scrollView)
        }
        textView__5.setOnClickListener {
            scrollToView(textView10, scrollView)
        }

        textView_1.setOnClickListener {
            scrollToView(textView__1, scrollView)
        }
        textView1.setOnClickListener {
            scrollToView(textView__1, scrollView)
        }
        textView8.setOnClickListener {
            scrollToView(textView__1, scrollView)
        }
        textView10.setOnClickListener {
            scrollToView(textView__1, scrollView)
        }
    }
}