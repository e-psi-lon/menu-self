package fr.e_psi_lon.menuself.activity


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import fr.e_psi_lon.menuself.BuildConfig
import fr.e_psi_lon.menuself.R
import fr.e_psi_lon.menuself.data.Menu
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone
import fr.e_psi_lon.menuself.data.Request as menuRequest

class ChangelogHistoryActivity : AppCompatActivity() {
    private lateinit var changelogHistoryLoading: TextView
    private lateinit var changelogHistory: ListView
    private lateinit var exitButton: ImageButton
    private var menus: MutableMap<String, Menu> = mutableMapOf()


    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog_history)
        changelogHistoryLoading = findViewById(R.id.history_status)
        changelogHistory = findViewById(R.id.changelog_history_list)
        exitButton = findViewById(R.id.exit_button)
        if (intent.hasExtra("evening")) {
            menus["evening"] = Menu.fromJson(intent.getStringExtra("evening")!!)
        }
        if (intent.hasExtra("noon")) {
            menus["noon"] = Menu.fromJson(intent.getStringExtra("noon")!!)
        }

        if (menuRequest.isNetworkAvailable(this)) {
            runBlocking {
                GlobalScope.launch(Dispatchers.IO) {
                    loadHistory()
                }
            }
        } else {
            changelogHistoryLoading.text = getString(R.string.no_internet)
            changelogHistoryLoading.visibility = View.VISIBLE
            changelogHistory.visibility = View.GONE
        }

        exitButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            if (menus.containsKey("evening")) {
                intent.putExtra("evening", menus["evening"]!!.toJson())
            }
            if (::changelogHistory.isInitialized) {
                intent.putExtra("noon", menus["noon"]!!.toJson())
            }
            startActivity(intent).apply {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.dont_move, R.anim.slide_out_bottom)
            }.also { finish() }
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        val intent = Intent(this, SettingsActivity::class.java)
        if (menus.containsKey("evening")) {
            intent.putExtra("evening", menus["evening"]?.toJson())
        }
        if (menus.containsKey("noon")) {
            intent.putExtra("noon", menus["noon"]?.toJson())
        }
        startActivity(intent).apply {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.dont_move, R.anim.slide_out_bottom)
        }.also { finish() }
    }

    private fun loadHistory() {
        try {
            val client = OkHttpClient()
            var request = Request.Builder()
                .url("https://api.github.com/repos/e-psi-lon/menu-self/branches/master")
                .build()
            val master = client.newCall(request).execute().body!!.string()
            val masterJson = JSONObject(master)
            val masterCommit = masterJson.getJSONObject("commit")
            val masterCommitSha = masterCommit.getString("sha")
            request = Request.Builder()
                .url("https://api.github.com/repos/e-psi-lon/menu-self/commits?sha=$masterCommitSha&per_page=100")
                .build()
            val commits = client.newCall(request).execute().body!!.string()
            val commitsJson = JSONArray(commits)
            val changelogHistoryList = ArrayList<String>()
            for (i in 0 until commitsJson.length()) {
                val commit = commitsJson.getJSONObject(i)
                val commitSha = commit.getString("sha").substring(0, 7)
                val commitMessage = commit.getJSONObject("commit").getString("message")
                val author = commit.getJSONObject("commit").getJSONObject("author")
                val authorName = author.getString("name")
                val date = author.getString("date")
                val dateSplit = date.split("T")[0].split("-")
                val hourSplit = date.split("T")[1].split(":").toMutableList()
                val timeZone = TimeZone.getDefault()
                val offset = timeZone.getOffset(System.currentTimeMillis())
                val offsetHours = offset / 1000 / 60 / 60
                hourSplit[0] = (hourSplit[0].toInt() + offsetHours).toString()
                val dateFormatted =
                    "${dateSplit[2]}/${dateSplit[1]}/${dateSplit[0]} - ${hourSplit[0]}h${hourSplit[1]}"
                changelogHistoryList.add(
                    if (commitSha == BuildConfig.GIT_COMMIT_HASH.substring(0, 7)) {
                        "$commitSha (${getString(R.string.actual)}) - ${
                            getString(
                                R.string.published,
                                dateFormatted
                            )
                        }\n$commitMessage - $authorName"
                    } else {
                        "$commitSha - ${
                            getString(
                                R.string.published,
                                dateFormatted
                            )
                        }\n$commitMessage - $authorName"
                    }
                )
            }
            runOnUiThread {
                changelogHistory.adapter = ArrayAdapter(
                    this@ChangelogHistoryActivity,
                    android.R.layout.simple_list_item_1,
                    changelogHistoryList
                )
                changelogHistory.visibility = View.VISIBLE
                changelogHistoryLoading.visibility = View.GONE

            }
        } catch (e: Exception) {
            runOnUiThread {
                changelogHistoryLoading.text = getString(R.string.changelogHistoryError)
                changelogHistoryLoading.visibility = View.VISIBLE
                changelogHistory.visibility = View.GONE
            }
        }
    }

}