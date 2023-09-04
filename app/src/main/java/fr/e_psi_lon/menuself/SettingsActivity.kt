package fr.e_psi_lon.menuself

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var settingsButton: ImageButton
    private lateinit var eveningButton: ImageButton
    private lateinit var noonButton: ImageButton
    private lateinit var versionView: TextView
    private lateinit var layout: LinearLayout
    private var appVersionName: String = BuildConfig.VERSION_NAME
    private var currentPage: String = "settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        layout = findViewById(R.id.mainLayout)
        noonButton = findViewById(R.id.noonButton)
        eveningButton = findViewById(R.id.eveningButton)
        settingsButton = findViewById(R.id.settingsButton)
        versionView = findViewById(R.id.version)
        versionView.text = getString(R.string.version, appVersionName)

        // TODO: Add a button to clear the cache
        // TODO: Add a button to download latest apk in downloads folder
        // TODO: Add a button to check for updates


        noonButton.setOnClickListener {
            if (currentPage != "noon") {
                changePage(MainActivity::class.java, mapOf("currentPage" to "noon"))
            }
        }

        eveningButton.setOnClickListener {
            if (currentPage != "evening") {
                changePage(MainActivity::class.java, mapOf("currentPage" to "evening"))
            }
        }

        settingsButton.setOnClickListener {
            if (currentPage != "settings") {
                changePage(MainActivity::class.java, mapOf("currentPage" to "settings"))
            }
        }

    }

    private fun changePage(page: Class<*>, extras: Map<String, String>) {
        val intent = Intent(this, page)
        for (extra in extras) {
            intent.putExtra(extra.key, extra.value)
        }
        // We define the animation (left to right or right to left)
        val list = listOf("noon", "evening", "settings")
        val index = list.indexOf(currentPage)
        startActivity(intent).apply {
            if (index < list.indexOf(extras["currentPage"])) {
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }.also {
            finish()
        }
    }
}