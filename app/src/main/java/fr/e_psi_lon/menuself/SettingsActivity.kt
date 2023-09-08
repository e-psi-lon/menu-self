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
    private lateinit var latestChangelogButton: Button
    private lateinit var changelogHistoryButton: Button
    private lateinit var eveningMenu: Menu
    private lateinit var noonMenu: Menu
    private var appVersionName: String = BuildConfig.VERSION_NAME
    private var currentPage: String = "settings"
    private var filename: String = ""

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
        latestChangelogButton = findViewById(R.id.changelogButton)
        changelogHistoryButton = findViewById(R.id.changelogHistoryButton)
        versionView.text = getString(R.string.version, appVersionName)
        if (intent.hasExtra("eveningMenu")) {
            eveningMenu = Menu.fromJson(intent.getStringExtra("eveningMenu")!!)
        }
        if (intent.hasExtra("noonMenu")) {
            noonMenu = Menu.fromJson(intent.getStringExtra("noonMenu")!!)
        }
        resetCacheButton.setOnClickListener {
            val cacheDir = File("Android/data/fr.e_psi_lon.menuself/cache")
            cacheDir.deleteRecursively()
            cacheDir.mkdir()
            changePage(MainActivity::class.java, mapOf("currentPage" to "settings"), false)
        }

        downloadLatestApkButton.setOnClickListener {
            askUserForBrowserOrApp().show()
        }

        checkForUpdatesButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                checkVersion()
            }
        }

        noonButton.setOnClickListener {
            if (currentPage != "noon") {
                val extras = mutableMapOf<String, String>()
                extras["currentPage"] = "noon"
                if (::eveningMenu.isInitialized) {
                    extras["eveningMenu"] = eveningMenu.toJson()
                }
                if (::noonMenu.isInitialized) {
                    extras["noonMenu"] = noonMenu.toJson()
                }
                changePage(MainActivity::class.java, extras)
            }
        }

        eveningButton.setOnClickListener {
            if (currentPage != "evening") {
                val extras = mutableMapOf<String, String>()
                extras["currentPage"] = "evening"
                if (::eveningMenu.isInitialized) {
                    extras["eveningMenu"] = eveningMenu.toJson()
                }
                if (::noonMenu.isInitialized) {
                    extras["noonMenu"] = noonMenu.toJson()
                }
                changePage(MainActivity::class.java, extras)
            }
        }
        latestChangelogButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                val output =
                    Request.get("https://api.github.com/repos/e-psi-lon/menu-self/commits/master")
                val changelog = if (output == "") {
                    getString(R.string.no_changelog)
                } else {
                    val json = JSONObject(output)
                    val commit = json.getJSONObject("commit")
                    commit.getString("message")
                }
                runOnUiThread {
                    val builder = AlertDialog.Builder(this@SettingsActivity)
                    builder.apply {
                        setTitle(R.string.changelog_is)
                        setMessage(changelog)
                        setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.cancel()
                        }
                    }
                    builder.create().show()
                }
            }
        }
        changelogHistoryButton.setOnClickListener {
            val intent = Intent(this, ChangelogHistoryActivity::class.java)
            if (::eveningMenu.isInitialized) {
                intent.putExtra("eveningMenu", eveningMenu.toJson())
            }
            if (::noonMenu.isInitialized) {
                intent.putExtra("noonMenu", noonMenu.toJson())
            }
            startActivity(intent).apply { }.also { finish() }
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
                    GlobalScope.launch(Dispatchers.IO) {
                        val url = getUrl()
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = url[0].toUri()
                        startActivity(intent).also {
                            finish()
                        }
                    }
                }
                setNegativeButton(R.string.app) { _, _ ->
                    GlobalScope.launch(Dispatchers.IO) {
                        val url = getUrl()
                        runOnUiThread {
                            askFilenameToUser().show()
                        }
                        while (filename == "") {
                            Thread.sleep(100)
                        }
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "$filename.apk"
                        )
                        Request.download(
                            url[0],
                            this@SettingsActivity.applicationContext,
                            this@SettingsActivity,
                            file,
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                            url[1].toLong()
                        )
                    }
                }
            }
            builder.create()
        }
    }

    private fun changePage(page: Class<*>, extras: Map<String, String>, animation: Boolean = true) {
        val intent = Intent(this, page)
        for (extra in extras) {
            intent.putExtra(extra.key, extra.value)
        }
        val list = listOf("noon", "evening", "settings")
        val index = list.indexOf(currentPage)
        startActivity(intent).apply {
            if (animation) {
                if (index < list.indexOf(extras["currentPage"])) {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }.also {
            finish()
        }
    }

    private fun checkVersion() {
        if (AutoUpdater.getLastCommitHash() != BuildConfig.GIT_COMMIT_HASH) {
            AutoUpdater().show(supportFragmentManager, "AutoUpdater")
        } else {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.up_to_date), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun askFilenameToUser(): AlertDialog {
        return this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setView(R.layout.dialog_filename)
                setPositiveButton(R.string.ok) { _, _ ->
                    val dialog = builder.create()
                    val filenameView = dialog.findViewById<TextView>(R.id.filename)
                    filename = if (filenameView?.text.toString() == "") {
                        getString(R.string.app_name)
                    } else {
                        filenameView?.text.toString()
                    }
                }
            }
            builder.create()
        }
    }
}