package fr.e_psi_lon.menuself

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity: AppCompatActivity() {
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


        // Gérer les clics sur les boutons de navigation
        noonButton.setOnClickListener {
            if (currentPage != "noon") {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("currentPage", "noon")
                })
                finish()
            }
        }

        eveningButton.setOnClickListener {
            if (currentPage != "evening") {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("currentPage", "evening")
                })
                finish()
            }
        }

        settingsButton.setOnClickListener {
            // Faire quelque chose lorsque le bouton "Paramètres" est cliqué
            if (currentPage != "settings") {
                val intent = Intent(this, SettingsActivity::class.java)
                // We start the activity
                intent.putExtra("currentPage", "settings")
                startActivity(intent)
                finish()
            }
        }

    }
}