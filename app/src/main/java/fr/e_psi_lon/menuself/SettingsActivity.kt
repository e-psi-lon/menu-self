package fr.e_psi_lon.menuself

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
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
    private lateinit var moreInfoButton: Button
    private lateinit var initActivitySpinner: Spinner
    private lateinit var updateBranchSpinner: Spinner
    private lateinit var config: JSONObject
    private lateinit var eveningMenu: Menu
    private lateinit var noonMenu: Menu
    private var appVersionName: String = BuildConfig.VERSION_NAME

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
        moreInfoButton = findViewById(R.id.moreInfoButton)
        initActivitySpinner = findViewById(R.id.initActivitySpinner)
        updateBranchSpinner = findViewById(R.id.updateBranchSpinner)
        val channel = mapOf(
            "dev" to getString(R.string.dev),
            "alpha" to getString(R.string.alpha),
            "beta" to getString(R.string.beta),
            "stable" to getString(R.string.stable)
        )
        val activity = mapOf(
            "NoonActivity" to getString(R.string.noon),
            "EveningActivity" to getString(R.string.evening),
            "SettingsActivity" to getString(R.string.settings),
            "previous" to getString(R.string.restart_where_you_left)
        )
        val activityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            activity.values.toList()
        )
        val channelAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            channel.values.toList()
        )
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        initActivitySpinner.adapter = activityAdapter
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        updateBranchSpinner.adapter = channelAdapter

        versionView.text = getString(R.string.version, appVersionName)
        if (intent.hasExtra("eveningMenu")) {
            eveningMenu = Menu.fromJson(intent.getStringExtra("eveningMenu")!!)
        }
        if (intent.hasExtra("noonMenu")) {
            noonMenu = Menu.fromJson(intent.getStringExtra("noonMenu")!!)
        }
        config = JSONObject(File(filesDir, "config.json").readText())
        if (config.getString("defaultActivity") == "previous") {
            config.put("previousActivity", "SettingsActivity")
            File(filesDir, "config.json").writeText(config.toString())
        }
        initActivitySpinner.setSelection(
            activity.keys.toList().indexOf(config.getString("defaultActivity"))
        )
        if (intent.hasExtra("init") && Request.isNetworkAvailable(applicationContext)) {
            GlobalScope.launch(Dispatchers.IO) {
                AutoUpdater.checkForUpdates(
                    this@SettingsActivity,
                    config.getString("updateChannel")
                )
            }
        }

        moreInfoButton.setOnClickListener {
            if (Request.isNetworkAvailable(applicationContext)) {
                val builder = AlertDialog.Builder(this)
                builder.apply {
                    setTitle(R.string.more_menu_info)
                    if (::eveningMenu.isInitialized) {
                        if (eveningMenu.redactionMessage != null) {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text_redaction_message,
                                    eveningMenu.lastUpdate,
                                    eveningMenu.nextUpdate,
                                    eveningMenu.redactionMessage
                                )
                            )
                        } else {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text,
                                    eveningMenu.lastUpdate,
                                    eveningMenu.nextUpdate
                                )
                            )
                        }
                    } else if (::noonMenu.isInitialized) {
                        if (noonMenu.redactionMessage != null) {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text_redaction_message,
                                    noonMenu.lastUpdate,
                                    noonMenu.nextUpdate,
                                    noonMenu.redactionMessage
                                )
                            )
                        } else {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text,
                                    noonMenu.lastUpdate,
                                    noonMenu.nextUpdate
                                )
                            )
                        }
                    } else {
                        setMessage(getString(R.string.more_menu_info_text_no_menu))
                    }
                    setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.cancel()
                    }
                }
                runOnUiThread {
                    builder.create().show()
                }
            } else {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }

        resetCacheButton.setOnClickListener {
            val cacheDir = File("Android/data/fr.e_psi_lon.menuself/cache")
            cacheDir.deleteRecursively()
            cacheDir.mkdir()
            changePage(MainActivity::class.java, mapOf(), false)
        }

        downloadLatestApkButton.setOnClickListener {
            if (Request.isNetworkAvailable(applicationContext)) {
                askUserForBrowserOrApp().show()
            } else {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }

        checkForUpdatesButton.setOnClickListener {
            if (Request.isNetworkAvailable(applicationContext)) {
                GlobalScope.launch(Dispatchers.IO) {
                    if (!AutoUpdater.checkForUpdates(
                            this@SettingsActivity,
                            config.getString("updateChannel")
                        )
                    ) {
                        runOnUiThread {
                            Toast.makeText(
                                this@SettingsActivity,
                                getString(R.string.up_to_date),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }

        noonButton.setOnClickListener {
            val extras = mutableMapOf<String, String>()
            if (::eveningMenu.isInitialized) {
                extras["eveningMenu"] = eveningMenu.toJson()
            }
            if (::noonMenu.isInitialized) {
                extras["noonMenu"] = noonMenu.toJson()
            }
            changePage(NoonActivity::class.java, extras)
        }

        eveningButton.setOnClickListener {
            val extras = mutableMapOf<String, String>()
            if (::eveningMenu.isInitialized) {
                extras["eveningMenu"] = eveningMenu.toJson()
            }
            if (::noonMenu.isInitialized) {
                extras["noonMenu"] = noonMenu.toJson()
            }
            changePage(EveningActivity::class.java, extras)
        }
        latestChangelogButton.setOnClickListener {
            if (Request.isNetworkAvailable(applicationContext)) {
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
            } else {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
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
            startActivity(intent).apply {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_top, R.anim.dont_move)
            }.also { finish() }
        }

        initActivitySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                pos: Int,
                id: Long
            ) {
                config.put("defaultActivity", activity.keys.toList()[pos])
                if (activity.keys.toList()[pos] == "previous") {
                    config.put("previousActivity", this@SettingsActivity::class.java.simpleName)
                }
                File(filesDir, "config.json").writeText(config.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        updateBranchSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                pos: Int,
                id: Long
            ) {
                config.put("updateChannel", channel.keys.toList()[pos])
                File(filesDir, "config.json").writeText(config.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun getUrl(): List<String> {
        val repoOutput =
            Request.get(
                "https://api.github.com/repos/e-psi-lon/menu-self/commits/builds-${
                    config.getString(
                        "updateChannel"
                    )
                }"
            )
        return try {
            val repoJson = JSONObject(repoOutput)
            val contentUrl =
                repoJson.getJSONArray("files").getJSONObject(0).getString("contents_url")
            val contentOutput = Request.get(contentUrl)
            val contentJson = JSONObject(contentOutput)
            listOf(contentJson.getString("download_url"), contentJson.getLong("size").toString())
        } catch (e: Exception) {
            listOf("", "")
        }
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
                        if (url[0] == "") {
                            return@launch
                        }
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
                        if (url[0] == "") {
                            return@launch
                        }
                        Request.download(
                            url[0],
                            this@SettingsActivity.applicationContext,
                            this@SettingsActivity,
                            File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                getString(R.string.app_name) + ".apk"
                            ),
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
        val map = mapOf(
            "NoonActivity" to 0,
            "EveningActivity" to 1,
            "SettingsActivity" to 2
        )
        val index = 2
        startActivity(intent).apply {
            if (animation) {
                if (index < map[page.simpleName]!!) {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }.also {
            finish()
        }
    }
}