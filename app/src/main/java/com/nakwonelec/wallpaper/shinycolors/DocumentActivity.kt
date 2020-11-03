package com.nakwonelec.wallpaper.shinycolors

import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

open class DocumentActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /// https://dreamaz.tistory.com/109
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                //toolbar의 back키 눌렀을 때 동작
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /// https://kanzler.tistory.com/310
    fun scrollToView(view: View?, scrollView: ScrollView?, count: Int = 0) {
        var count = count
        if(view != null && view != scrollView) {
            count += view.top
            scrollToView(view.parent as View, scrollView, count)
        } else if (scrollView != null) {
            val finalCount = count
            scrollView.post {
                scrollView.smoothScrollTo(0, finalCount)
            }
        }
    }
}