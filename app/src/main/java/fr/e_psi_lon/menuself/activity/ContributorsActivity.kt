package fr.e_psi_lon.menuself.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import de.hdodenhof.circleimageview.CircleImageView
import fr.e_psi_lon.menuself.R
import fr.e_psi_lon.menuself.data.Menu
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import fr.e_psi_lon.menuself.data.Request as menuRequest

class ContributorsActivity : AppCompatActivity() {
    private lateinit var contributorsLoading: TextView
    private lateinit var scrollLinearLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var exitButton: ImageButton
    private var menus: MutableMap<String, Menu> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contributors)
        scrollLinearLayout = findViewById(R.id.contributors_list_layout)
        contributorsLoading = findViewById(R.id.loading_contributors)
        scrollView = findViewById(R.id.contributors_list)
        exitButton = findViewById(R.id.exit_button)
        if (intent.hasExtra("evening")) {
            menus["evening"] = Menu.fromJson(intent.getStringExtra("evening")!!)
        }
        if (intent.hasExtra("noon")) {
            menus["noon"] = Menu.fromJson(intent.getStringExtra("noon")!!)
        }

        scrollLinearLayout.isVerticalScrollBarEnabled = false
        if (menuRequest.isNetworkAvailable(this)) {
            loadContent()
        } else {
            showNoInternetError()
        }

        exitButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            if (menus.containsKey("evening")) {
                intent.putExtra("evening", menus["evening"]?.toJson())
            }
            if (menus.containsKey("noon")) {
                intent.putExtra("noon", menus["noon"]?.toJson())
            }
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.dont_move, R.anim.slide_out_bottom)
            finish()
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
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.dont_move, R.anim.slide_out_bottom)
        finish()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadContent() {
        contributorsLoading.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val contributors = getContributors()
                val contributorsInfo = parseContributors(contributors)
                contributorsInfo.sortByDescending { it["contributions"] as Int }
                withContext(Dispatchers.Main) {
                    showContributors(contributorsInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError()
                }
            }
        }
    }

    private fun getContributors(): JSONArray {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.github.com/repos/e-psi-lon/menu-self/contributors")
            .build()
        val response = client.newCall(request).execute()
        val contributorsText = response.body.string()
        return JSONArray(contributorsText)
    }

    private fun parseContributors(contributors: JSONArray): MutableList<Map<String, Any?>> {
        val contributorsInfo = mutableListOf<Map<String, Any?>>()
        for (i in 0 until contributors.length()) {
            val contributor = contributors.getJSONObject(i)
            val contributorInfo = mapOf(
                "login" to contributor.getString("login"),
                "avatar" to menuRequest.getImage(contributor.getString("avatar_url")),
                "html_url" to contributor.getString("html_url"),
                "contributions" to contributor.getInt("contributions")
            )
            contributorsInfo.add(contributorInfo)
        }
        return contributorsInfo
    }

    private fun showContributors(contributorsInfo: MutableList<Map<String, Any?>>) {
        contributorsLoading.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        scrollLinearLayout.visibility = View.VISIBLE
        for (contributor in contributorsInfo) {
            val contributorLayout =
                layoutInflater.inflate(R.layout.contributor, scrollLinearLayout)
            val contributorName =
                contributorLayout.findViewById<TextView>(R.id.contributor_name)
            val contributorAvatar =
                contributorLayout.findViewById<CircleImageView>(R.id.contributor_image)
            val contributorContributions =
                contributorLayout.findViewById<TextView>(R.id.contribution_count)
            contributorName.text = contributor["login"] as String
            val image = contributor["avatar"] as Bitmap? ?: BitmapFactory.decodeResource(
                resources,
                R.mipmap.no_image
            )
            contributorAvatar.setImageBitmap(image)
            contributorContributions.text =
                getString(R.string.contributions, contributor["contributions"] as String)
            contributorAvatar.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = contributor["html_url"].toString().toUri()
                startActivity(intent)
            }
        }
    }

    private fun showError() {
        contributorsLoading.text = getString(R.string.error_loading_contributors)
        contributorsLoading.visibility = View.VISIBLE
        scrollView.visibility = View.GONE
        scrollLinearLayout.visibility = View.GONE
    }

    private fun showNoInternetError() {
        contributorsLoading.text = getString(R.string.no_internet)
        contributorsLoading.visibility = View.VISIBLE
        scrollLinearLayout.visibility = View.GONE
        scrollView.visibility = View.GONE
    }
}