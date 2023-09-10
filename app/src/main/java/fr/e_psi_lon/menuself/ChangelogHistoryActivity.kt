package fr.e_psi_lon.menuself


import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ChangelogHistoryActivity : AppCompatActivity() {
    private lateinit var changelogHistoryLoading: TextView
    private lateinit var changelogHistory: ListView
    private lateinit var exitButton: ImageButton
    private lateinit var eveningMenu: Menu
    private lateinit var noonMenu: Menu


    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog_history)
        changelogHistoryLoading = findViewById(R.id.historyStatus)
        changelogHistory = findViewById(R.id.changelogHistoryList)
        exitButton = findViewById(R.id.exitButton)
        if (intent.hasExtra("eveningMenu")) {
            eveningMenu = Menu.fromJson(intent.getStringExtra("eveningMenu")!!)
        }
        if (intent.hasExtra("noonMenu")) {
            noonMenu = Menu.fromJson(intent.getStringExtra("noonMenu")!!)
        }

        if (isNetworkAvailable()) {
            GlobalScope.launch(Dispatchers.IO) {
                loadHistory()
            }
        } else {
            changelogHistoryLoading.text = getString(R.string.no_internet)
            changelogHistoryLoading.visibility = View.VISIBLE
            changelogHistory.visibility = View.GONE
        }

        exitButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            if (::eveningMenu.isInitialized) {
                intent.putExtra("eveningMenu", eveningMenu.toJson())
            }
            if (::noonMenu.isInitialized) {
                intent.putExtra("noonMenu", noonMenu.toJson())
            }
            startActivity(intent).apply {
                overridePendingTransition(R.anim.dont_move, R.anim.slide_out_bottom)
            }.also { finish() }
        }

    }

    private fun loadHistory() {
        try {
            val master =
                Request.get("https://api.github.com/repos/e-psi-lon/menu-self/branches/master")
            val masterJson = JSONObject(master)
            val masterCommit = masterJson.getJSONObject("commit")
            val masterCommitSha = masterCommit.getString("sha")
            val commits =
                Request.get("https://api.github.com/repos/e-psi-lon/menu-self/commits?sha=$masterCommitSha")
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
                val dateFormatted = "${dateSplit[2]}/${dateSplit[1]}/${dateSplit[0]}"
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetwork
        return activeNetworkInfo != null
    }
}