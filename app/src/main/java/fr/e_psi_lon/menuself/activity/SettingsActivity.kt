package fr.e_psi_lon.menuself.activity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import fr.e_psi_lon.menuself.BuildConfig
import fr.e_psi_lon.menuself.R
import fr.e_psi_lon.menuself.data.Menu
import fr.e_psi_lon.menuself.others.AutoUpdater
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import fr.e_psi_lon.menuself.data.Request as menuRequest

class SettingsActivity : AppCompatActivity() {
    private lateinit var settingsButton: ImageButton
    private lateinit var eveningButton: ImageButton
    private lateinit var noonButton: ImageButton
    private lateinit var versionView: TextView
    private lateinit var layout: LinearLayout
    private lateinit var downloadLatestApkButton: Button
    private lateinit var checkForUpdatesButton: Button
    private lateinit var changelogHistoryButton: Button
    private lateinit var moreInfoButton: Button
    private lateinit var contributorsButton: Button
    private lateinit var changeApiUrl: Button
    private lateinit var initActivitySpinner: Spinner
    private lateinit var updateBranchSpinner: Spinner
    private lateinit var usePronote: SwitchCompat
    private lateinit var disconnectFromPronote: Button
    private lateinit var config: JSONObject
    private var menus: MutableMap<String, Menu> = mutableMapOf()
    private var appVersionName: String = BuildConfig.VERSION_NAME

    @SuppressLint("InflateParams")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        layout = findViewById(R.id.mainLayout)
        noonButton = findViewById(R.id.noonButton)
        eveningButton = findViewById(R.id.eveningButton)
        settingsButton = findViewById(R.id.settingsButton)
        versionView = findViewById(R.id.version)
        downloadLatestApkButton = findViewById(R.id.downloadApkButton)
        checkForUpdatesButton = findViewById(R.id.checkUpdateButton)
        changelogHistoryButton = findViewById(R.id.changelogHistoryButton)
        moreInfoButton = findViewById(R.id.moreInfoButton)
        initActivitySpinner = findViewById(R.id.initActivitySpinner)
        updateBranchSpinner = findViewById(R.id.updateBranchSpinner)
        contributorsButton = findViewById(R.id.contributorsButton)
        changeApiUrl = findViewById(R.id.changeAPIURLButton)
        usePronote = findViewById(R.id.usePronoteSwitch)
        disconnectFromPronote = findViewById(R.id.disconnectPronoteButton)
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
        if (intent.hasExtra("evening")) {
            menus["evening"] = Menu.fromJson(intent.getStringExtra("evening")!!)
        }
        if (intent.hasExtra("noon")) {
            menus["noon"] = Menu.fromJson(intent.getStringExtra("noon")!!)
        }
        config = JSONObject(File(filesDir, "config.json").readText())
        if (config.getString("defaultActivity") == "previous") {
            config.put("previousActivity", "SettingsActivity")
            File(filesDir, "config.json").writeText(config.toString())
        }
        initActivitySpinner.setSelection(
            activity.keys.toList().indexOf(config.getString("defaultActivity"))
        )
        disconnectFromPronote.visibility = if (config.getBoolean("usePronote")) {
            View.VISIBLE
        } else {
            View.GONE
        }
        if (intent.hasExtra("init") && menuRequest.isNetworkAvailable(applicationContext)) {
            GlobalScope.launch(Dispatchers.IO) {
                AutoUpdater.checkForUpdates(
                    this@SettingsActivity,
                    config.getString("updateChannel")
                )
            }
        }

        moreInfoButton.setOnClickListener {
            if (menuRequest.isNetworkAvailable(applicationContext)) {
                val builder = AlertDialog.Builder(this)
                builder.apply {
                    setTitle(R.string.more_menu_info)
                    if (menus.containsKey("evening")) {
                        if (menus["evening"]?.redactionMessage != null) {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text_redaction_message,
                                    menus["evening"]?.lastUpdate ?: "",
                                    menus["evening"]?.nextUpdate ?: "",
                                    menus["evening"]?.redactionMessage ?: ""
                                )
                            )
                        } else {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text,
                                    menus["evening"]?.lastUpdate ?: "",
                                    menus["evening"]?.nextUpdate ?: ""
                                )
                            )
                        }
                    } else if (menus.containsKey("noon")) {
                        if (menus["noon"]?.redactionMessage != null) {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text_redaction_message,
                                    menus["noon"]?.lastUpdate ?: "",
                                    menus["noon"]?.nextUpdate ?: "",
                                    menus["noon"]?.redactionMessage ?: ""
                                )
                            )
                        } else {
                            setMessage(
                                getString(
                                    R.string.more_menu_info_text,
                                    menus["noon"]?.lastUpdate ?: "",
                                    menus["noon"]?.nextUpdate ?: ""
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

        downloadLatestApkButton.setOnClickListener {
            if (menuRequest.isNetworkAvailable(applicationContext)) {
                askUserForBrowserOrApp().show()
            } else {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }



        checkForUpdatesButton.setOnClickListener {
            if (menuRequest.isNetworkAvailable(applicationContext)) {
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
            if (menus.containsKey("evening")) {
                extras["evening"] = menus["evening"]!!.toJson()
            }
            if (menus.containsKey("noon")) {
                extras["noon"] = menus["noon"]!!.toJson()
            }
            changePage(NoonActivity::class.java, extras)
        }

        eveningButton.setOnClickListener {
            val extras = mutableMapOf<String, String>()
            if (menus.containsKey("evening")) {
                extras["evening"] = menus["evening"]!!.toJson()
            }
            if (menus.containsKey("noon")) {
                extras["noon"] = menus["noon"]!!.toJson()
            }
            changePage(EveningActivity::class.java, extras)
        }

        changelogHistoryButton.setOnClickListener {
            val intent = Intent(this, ChangelogHistoryActivity::class.java)
            if (menus.containsKey("evening")) {
                intent.putExtra("evening", menus["evening"]!!.toJson())
            }
            if (menus.containsKey("noon")) {
                intent.putExtra("noon", menus["noon"]!!.toJson())
            }
            startActivity(intent).apply {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_top, R.anim.dont_move)
            }.also { finish() }
        }

        contributorsButton.setOnClickListener {
            val intent = Intent(this, ContributorsActivity::class.java)
            if (menus.containsKey("evening")) {
                intent.putExtra("evening", menus["evening"]!!.toJson())
            }
            if (menus.containsKey("noon")) {
                intent.putExtra("noon", menus["noon"]!!.toJson())
            }
            startActivity(intent).apply {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_top, R.anim.dont_move)
            }.also { finish() }
        }

        changeApiUrl.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.change_api_url, null)
            val builder = AlertDialog.Builder(this)
            builder.apply {
                dialogView.findViewById<EditText>(R.id.apiUrl).setText(
                    config.getString("pronoteAPI")
                )
                setView(dialogView)
                setCancelable(true)
                setPositiveButton(R.string.change) { dialog, _ ->
                    val url = dialogView.findViewById<EditText>(R.id.apiUrl)
                    var urlText = url.text.toString()
                    if (urlText != "") {
                        if (!url.text.toString().startsWith("http")) {
                            urlText = "https://${url.text}"
                        }
                        if (urlText.endsWith("/")) {
                            urlText = urlText.substring(0, urlText.length - 1)
                        }
                        // On essaye d'obtenir un token pour vérifier que l'URL est valide
                        GlobalScope.launch(Dispatchers.IO) {
                            val client = OkHttpClient()
                            val request = Request.Builder()
                                .url(
                                    "${urlText}/generatetoken"
                                )
                                .post(
                                    JSONObject(
                                        mapOf(
                                            "username" to "demonstration",
                                            "password" to "pronotevs",
                                            "url" to "https://demo.index-education.net/pronote/eleve.html"
                                        )
                                    ).toString()
                                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                )
                                .build()
                            val response = client.newCall(request).execute()
                            if (response.code != 200) {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@SettingsActivity,
                                        getString(R.string.change_api_url_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                config.put("pronoteAPI", urlText)
                                File(filesDir, "config.json").writeText(config.toString())
                                runOnUiThread {
                                    Toast.makeText(
                                        this@SettingsActivity,
                                        getString(R.string.change_api_url_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        }
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.change_api_url_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                show()
            }
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
                if (config.getString("updateChannel") == channel.keys.toList()[pos]) {
                    return
                }
                val builder = AlertDialog.Builder(this@SettingsActivity)
                builder.apply {
                    setTitle(R.string.change_update_channel)
                    setPositiveButton(R.string.yes) { _, _ ->
                        config.put("updateChannel", channel.keys.toList()[pos])
                        File(filesDir, "config.json").writeText(config.toString())
                        GlobalScope.launch(Dispatchers.IO) {
                            AutoUpdater.checkForUpdates(
                                this@SettingsActivity,
                                config.getString("updateChannel")
                            )
                        }
                    }
                    setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                        updateBranchSpinner.setSelection(
                            channel.keys.toList().indexOf(config.getString("updateChannel"))
                        )
                    }
                    show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (config.has("usePronote")) {
            usePronote.isChecked = config.getBoolean("usePronote")
        } else {
            config.put("usePronote", false)
            usePronote.isChecked = false
        }
        usePronote.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && config.getBoolean("usePronote")) {
                menus["evening"] = Menu()
                menus["noon"] = Menu()
            }
            if (!isChecked) {
                config.put("usePronote", false)
                File(filesDir, "config.json").writeText(config.toString())
                disconnectFromPronote.visibility = View.GONE
            }
            if (!menuRequest.isNetworkAvailable(applicationContext)) {
                Toast.makeText(
                    this,
                    getString(R.string.no_internet),
                    Toast.LENGTH_SHORT
                ).show()
                usePronote.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (isChecked && !config.has("pronoteUsername") && !config.has("pronotePassword")) {
                val dialogView = layoutInflater.inflate(R.layout.pronote_connection, null)
                val builder = AlertDialog.Builder(this)
                builder.apply {
                    setView(dialogView)
                    setCancelable(true)
                    setPositiveButton(R.string.pronote_connection_button) { dialog, _ ->
                        val username = dialogView.findViewById<EditText>(R.id.pronote_username)
                        val password = dialogView.findViewById<EditText>(R.id.pronote_password)
                        if (username.text.toString() != "" && password.text.toString() != "") {
                            GlobalScope.launch(Dispatchers.IO) {
                                val client = OkHttpClient()
                                val request = Request.Builder()
                                    .url(
                                        "${config.get("pronoteAPI")}/generatetoken"
                                    )
                                    .post(
                                        JSONObject(
                                            mapOf(
                                                "username" to username.text.toString(),
                                                "password" to password.text.toString(),
                                                "url" to "https://0410002e.index-education.net/pronote/eleve.html",
                                                "ent" to "ac_orleans_tours"
                                            )
                                        ).toString()
                                            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                    )
                                    .build()
                                val response = client.newCall(request).execute()
                                if (response.code != 200) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@SettingsActivity,
                                            getString(R.string.pronote_connection_error),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        usePronote.isChecked = false
                                    }
                                } else {
                                    config.put("pronoteUsername", username.text.toString())
                                    config.put("pronotePassword", password.text.toString())
                                    config.put("usePronote", true)
                                    File(filesDir, "config.json").writeText(config.toString())
                                    dialog.dismiss()
                                    runOnUiThread {
                                        disconnectFromPronote.visibility = View.VISIBLE
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@SettingsActivity,
                                getString(R.string.pronote_connection_error),
                                Toast.LENGTH_SHORT
                            ).show()
                            usePronote.isChecked = false
                        }
                    }
                    this.setOnCancelListener {
                        usePronote.isChecked = false
                    }
                    show()
                }
            } else if (isChecked) {
                config.put("usePronote", true)
                File(filesDir, "config.json").writeText(config.toString())
                disconnectFromPronote.visibility = View.VISIBLE
            }
        }

        disconnectFromPronote.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.apply {
                setTitle(R.string.disconnect_pronote)
                setMessage(R.string.disconnect_from_pronote_message)
                setPositiveButton(R.string.yes) { _, _ ->
                    config.remove("pronoteUsername")
                    config.remove("pronotePassword")
                    File(filesDir, "config.json").writeText(config.toString())
                    disconnectFromPronote.visibility = View.GONE
                    usePronote.isChecked = false
                    menus["evening"] = Menu()
                    menus["noon"] = Menu()
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.pronote_disconnected),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                show()
            }
        }
    }

    private fun getUrl(): List<String> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(
                "https://api.github.com/repos/e-psi-lon/menu-self/commits/builds-${
                    config.getString(
                        "updateChannel"
                    )
                }"
            )
            .build()
        val repoOutput = client.newCall(request).execute().body?.string() ?: ""
        return try {
            val repoJson = JSONObject(repoOutput)
            val contentUrl =
                repoJson.getJSONArray("files").getJSONObject(0).getString("contents_url")
            val contentOutput = client.newCall(
                Request.Builder()
                    .url(contentUrl)
                    .build()
            ).execute().body?.string() ?: ""
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
                        menuRequest.download(
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        finishAffinity()
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