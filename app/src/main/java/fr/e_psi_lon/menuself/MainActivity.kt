package fr.e_psi_lon.menuself

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.e_psi_lon.menuself.activity.EveningActivity
import fr.e_psi_lon.menuself.activity.NoonActivity
import fr.e_psi_lon.menuself.activity.SettingsActivity
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!File(filesDir, "config.json").exists()) {
            val config = JSONObject()
            config.put("defaultActivity", "NoonActivity")
            config.put("updateChannel", "dev")
            File(filesDir, "config.json").writeText(config.toString())
        }
        val config = JSONObject(File(filesDir, "config.json").readText())
        if (!config.has("defaultActivity")) {
            config.put("defaultActivity", "NoonActivity")
        }
        if (!config.has("updateChannel")) {
            config.put("updateChannel", "dev")
        }
        if (!config.has("previousActivity")) {
            config.put("previousActivity", "NoonActivity")
        }
        if (!config.has("usePronote")) {
            config.put("usePronote", false)
        }


        File(filesDir, "config.json").writeText(config.toString())
        val map = mapOf(
            "NoonActivity" to NoonActivity::class.java,
            "EveningActivity" to EveningActivity::class.java,
            "SettingsActivity" to SettingsActivity::class.java,
        )
        if (config.getString("defaultActivity") == "previous") {
            val intent = Intent(this, map[config.getString("previousActivity")]!!).apply {
                putExtra("init", true)
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, map[config.getString("defaultActivity")]!!).apply {
                putExtra("init", true)
            }
            startActivity(intent)
        }
    }

}
