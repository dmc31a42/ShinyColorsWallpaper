package com.nakwonelec.wallpaper.shinycolors

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_help.*

class HelpActivity : DocumentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textView_1.setOnClickListener {
            scrollToView(textView0, scrollView)
        }
        textView_2.setOnClickListener {
            scrollToView(textView10, scrollView)
        }

        textView0.setOnClickListener {
            scrollToView(textView_0, scrollView)
        }
        textView10.setOnClickListener {
            scrollToView(textView_0, scrollView)
        }

    }
}