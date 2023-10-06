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
import fr.e_psi_lon.menuself.data.Request
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray

class ContributorsActivity : AppCompatActivity() {
    private lateinit var contributorsLoading: TextView
    private lateinit var scrollLinearLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var exitButton: ImageButton
    private lateinit var eveningMenu: Menu
    private lateinit var noonMenu: Menu

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contributors)
        scrollLinearLayout = findViewById(R.id.contributorsListLayout)
        contributorsLoading = findViewById(R.id.loadingContributors)
        scrollView = findViewById(R.id.contributorsList)
        exitButton = findViewById(R.id.exitButton)
        if (intent.hasExtra("eveningMenu")) {
            eveningMenu = Menu.fromJson(intent.getStringExtra("eveningMenu")!!)
        }
        if (intent.hasExtra("noonMenu")) {
            noonMenu = Menu.fromJson(intent.getStringExtra("noonMenu")!!)
        }

        scrollLinearLayout.isVerticalScrollBarEnabled = false
        if (Request.isNetworkAvailable(this)) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    loadContent()
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        contributorsLoading.text = getString(R.string.error_loading_contributors)
                        contributorsLoading.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        scrollLinearLayout.visibility = View.GONE
                    }
                }
            }
        } else {
            contributorsLoading.text = getString(R.string.no_internet)
            contributorsLoading.visibility = View.VISIBLE
            scrollLinearLayout.visibility = View.GONE
            scrollView.visibility = View.GONE
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
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.dont_move, R.anim.slide_out_bottom)
            }.also { finish() }
        }

    }

    private fun loadContent() {
        val contributorsText =
            Request.get("https://api.github.com/repos/e-psi-lon/menu-self/contributors")
        println("contributorsText: $contributorsText")
        println(contributorsText)
        val contributors = JSONArray(contributorsText)
        val contributorsInfo = mutableListOf<Map<String, Any?>>()
        for (i in 0 until contributors.length()) {
            val contributor = contributors.getJSONObject(i)
            val contributorInfo = mapOf(
                "login" to contributor.getString("login"),
                "avatar" to Request.getImage(contributor.getString("avatar_url")),
                "html_url" to contributor.getString("html_url"),
                "contributions" to contributor.getInt("contributions")
            )
            contributorsInfo.add(contributorInfo)
        }
        contributorsInfo.sortByDescending { it["contributions"] as Int }
        runOnUiThread {
            contributorsLoading.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
            scrollLinearLayout.visibility = View.VISIBLE
            for (contributor in contributorsInfo) {
                val contributorLayout =
                    layoutInflater.inflate(R.layout.contributor, scrollLinearLayout)
                val contributorName =
                    contributorLayout.findViewById<TextView>(R.id.contributor_name)
                println("TextView checked")
                val contributorAvatar =
                    contributorLayout.findViewById<CircleImageView>(R.id.contributor_image)
                println("CircleImageView checked")
                val contributorContributions =
                    contributorLayout.findViewById<TextView>(R.id.contribution_count)
                println("TextView (count) checked")
                contributorName.text = contributor["login"] as String
                println("Name set")
                val image = contributor["avatar"] as Bitmap? ?: BitmapFactory.decodeResource(
                    resources,
                    R.mipmap.no_image
                )
                println("Image configured")
                contributorAvatar.setImageBitmap(image)
                println("Image set")
                contributorContributions.text =
                    getString(R.string.contributions, contributor["contributions"] as String)
                println("Contributions set")
                contributorAvatar.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = contributor["html_url"].toString().toUri()
                    startActivity(intent)
                }
                println("Listener set")
            }
            println("Contributors loaded")
        }
    }
}