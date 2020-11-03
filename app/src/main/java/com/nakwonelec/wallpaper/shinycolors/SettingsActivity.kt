package com.nakwonelec.wallpaper.shinycolors

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
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

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            /// Config category
            findPreference<SwitchPreference>(Settings.Boolean.Sound.toString())?.setOnPreferenceClickListener {
                val intent = Intent(intent.Settings.toString())
                intent.putExtra(Settings.Boolean.toString(), Settings.Boolean.Sound.toString())
                context?.let {
                    LocalBroadcastManager.getInstance(it).sendBroadcast(intent)
                }
                true
            }

            /// Command category
            findPreference<Preference>(Settings.Control.Reload.toString())?.setOnPreferenceClickListener {
                val intent = Intent(intent.Settings.toString())
                intent.putExtra(Settings.Control.toString(), Settings.Control.Reload.toString())
                context?.let {
                    LocalBroadcastManager.getInstance(it).sendBroadcast(intent)
                }
                true
            }
            findPreference<Preference>(Settings.Control.Back.toString())?.setOnPreferenceClickListener {
                val intent = Intent(intent.Settings.toString())
                intent.putExtra(Settings.Control.toString(), Settings.Control.Back.toString())
                context?.let {
                    LocalBroadcastManager.getInstance(it).sendBroadcast(intent)
                }
                true
            }

            /// preliminary config
            findPreference<Preference>("help")?.setOnPreferenceClickListener {
                context?.let {context->
                    val intent = Intent(context, HelpActivity::class.java)
                    startActivity(intent)
                }
                true
            }
            findPreference<Preference>("troubleshoot")?.setOnPreferenceClickListener {
                context?.let {context->
                    val intent = Intent(context, TroubleshootActivity::class.java)
                    startActivity(intent)
                }
                true
            }
            findPreference<Preference>("login")?.setOnPreferenceClickListener {
                context?.let {context->
                    val intent = Intent(context, LoginWebActivity::class.java)
                    startActivity(intent)
                }
                true
            }
            findPreference<Preference>("login")?.setOnPreferenceClickListener {
                context?.let {context->
                    val intent = Intent(context, LoginWebActivity::class.java)
                    startActivity(intent)
                }
                true
            }
            findPreference<Preference>("change_live_wallpaper")?.setOnPreferenceClickListener {
                context?.let {context->
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, MyWallpaperService::class.java))
                    startActivity(intent)
                }
                true
            }
        }
    }
}