package fr.e_psi_lon.menuself

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private lateinit var settingsButton: ImageButton
    private lateinit var eveningButton: ImageButton
    private lateinit var noonButton: ImageButton
    private lateinit var versionView: TextView
    private lateinit var layout: LinearLayout
    private lateinit var resetCacheButton: Button
    private lateinit var downloadLatestApkButton: Button
    private lateinit var checkForUpdatesButton: Button
    private var appVersionName: String = BuildConfig.VERSION_NAME
    private var currentPage: String = "settings"

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        layout = findViewById(R.id.mainLayout)
        noonButton = findViewById(R.id.noonButton)
        eveningButton = findViewById(R.id.eveningButton)
        settingsButton = findViewById(R.id.settingsButton)
        versionView = findViewById(R.id.version)
        resetCacheButton = findViewById(R.id.resetCacheButton)
        downloadLatestApkButton = findViewById(R.id.downloadApkButton)
        checkForUpdatesButton = findViewById(R.id.checkUpdateButton)
        versionView.text = getString(R.string.version, appVersionName)

        // TODO: Add a button to clear the cache
        resetCacheButton.setOnClickListener {
            // On supprime le dossier Android/data/fr.e_psi_lon.menuself/cache
            val cacheDir = File("Android/data/fr.e_psi_lon.menuself/cache")
            cacheDir.deleteRecursively()
            cacheDir.mkdir()
            changePage(MainActivity::class.java, mapOf("currentPage" to "settings"))
        }
        // TODO: Add a button to download latest apk in downloads folder
        downloadLatestApkButton.setOnClickListener {
            // On demande à l'utilisateur si il veut passer par le navigateur ou par l'application
            askUserForBrowserOrApp().show()
        }
        // TODO: Add a button to check for updates
        checkForUpdatesButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                checkVersion()
            }
        }

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

    private fun getUrl(): List<String> {
        val repoOutput =
            Request.get("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds")
        val repoJson = JSONObject(repoOutput)
        val contentUrl = repoJson.getJSONArray("files").getJSONObject(0).getString("contents_url")
        val contentOutput = Request.get(contentUrl)
        val contentJson = JSONObject(contentOutput)
        return listOf(contentJson.getString("download_url"), contentJson.getLong("size").toString())
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun askUserForBrowserOrApp(): AlertDialog {
        return this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(R.string.browser_or_app)
                setPositiveButton(R.string.browser) { _, _ ->
                    // On utilise une coroutine pour télécharger le fichier
                    GlobalScope.launch(Dispatchers.IO) {
                        val url = getUrl()
                        // On ouvre le navigateur avec l'url
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = url[0].toUri()
                        startActivity(intent).also {
                            finish()
                        }
                    }
                }
                setNegativeButton(R.string.app) { _, _ ->
                    // On utilise une coroutine pour télécharger le fichier
                    GlobalScope.launch(Dispatchers.IO) {
                        val url = getUrl()
                        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name) + ".apk")
                        Request.download(url[0], this@SettingsActivity.applicationContext, this@SettingsActivity, file, DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED, url[1].toLong())
                    }
                }
            }
            // Create the AlertDialog
            builder.create()
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

    private fun checkVersion() {
        if (AutoUpdater.getLastCommitHash() != BuildConfig.GIT_COMMIT_HASH) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Don't have permission to read external storage, canceling update",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@runOnUiThread
                }
            }
            AutoUpdater().show(supportFragmentManager, "AutoUpdater")
        }
        else {
            runOnUiThread {
                Toast.makeText(this, "You are up to date", Toast.LENGTH_SHORT).show()
            }
        }
    }
}