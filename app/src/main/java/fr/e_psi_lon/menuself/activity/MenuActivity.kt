package fr.e_psi_lon.menuself.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import fr.e_psi_lon.menuself.R
import fr.e_psi_lon.menuself.data.Menu
import fr.e_psi_lon.menuself.data.Request
import fr.e_psi_lon.menuself.others.AutoUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.TimeZone

open class MenuActivity(private var hour: Int, private var pageIndex: Int) : AppCompatActivity() {
    private lateinit var layout: LinearLayout
    private lateinit var noonButton: ImageButton
    private lateinit var eveningButton: ImageButton
    private lateinit var settingsButton: ImageButton
    internal lateinit var menuListView: ListView
    internal lateinit var statusView: TextView
    private lateinit var dayPlusButton: ImageButton
    internal lateinit var dayView: TextView
    private lateinit var dayMinusButton: ImageButton
    internal lateinit var menuLayout: SwipeRefreshLayout
    internal lateinit var eveningMenu: Menu
    internal lateinit var noonMenu: Menu
    private lateinit var config: JSONObject
    private val dayInWeek: List<String> =
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    internal var currentDay: String = getDayOfWeek()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        layout = findViewById(R.id.mainLayout)
        noonButton = findViewById(R.id.noonButton)
        eveningButton = findViewById(R.id.eveningButton)
        settingsButton = findViewById(R.id.settingsButton)
        menuListView = findViewById(R.id.menuListView)
        dayPlusButton = findViewById(R.id.nextButton)
        dayMinusButton = findViewById(R.id.previousButton)
        dayView = findViewById(R.id.dateTextView)
        statusView = findViewById(R.id.statusTextView)
        menuLayout = findViewById(R.id.mealLayout)
        if (Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"))
                .get(Calendar.HOUR_OF_DAY) >= hour
        ) {
            currentDay = dayInWeek[dayInWeek.indexOf(currentDay) + 1]
        }
        menuLayout.isRefreshing = true
        config = JSONObject(File(filesDir, "config.json").readText())
        if (config.getString("defaultActivity") == "previous") {
            config.put("previousActivity", this::class.java.simpleName)
            File(filesDir, "config.json").writeText(config.toString())
        }

        if (Request.isNetworkAvailable(this.applicationContext) && intent.hasExtra("init")) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    AutoUpdater.checkForUpdates(
                        this@MenuActivity,
                        config.getString("updateChannel")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        println("Page index: $pageIndex")
        if (pageIndex == 0) {
            noonButton.setBackgroundColor(getColor(R.color.colorSelectedPageBackground))
            eveningButton.setBackgroundColor(getColor(R.color.colorSecondaryVariant))
        } else {
            eveningButton.setBackgroundColor(getColor(R.color.colorSelectedPageBackground))
            noonButton.setBackgroundColor(getColor(R.color.colorSecondaryVariant))
        }
        settingsButton.setBackgroundColor(getColor(R.color.colorSecondaryVariant))
        if (intent.hasExtra("eveningMenu")) {
            eveningMenu = Menu.fromJson(intent.getStringExtra("eveningMenu")!!)
            showMenu(currentDay)
            if (intent.hasExtra("noonMenu")) {
                noonMenu = Menu.fromJson(intent.getStringExtra("noonMenu")!!)
            }
        } else {
            if (intent.hasExtra("noonMenu")) {
                noonMenu = Menu.fromJson(intent.getStringExtra("noonMenu")!!)
            }
            if (Request.isNetworkAvailable(this.applicationContext)) {
                try {
                    fetchMenuData()
                } catch (e: Exception) {
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.loading_error)
                }
            } else {
                menuLayout.isRefreshing = false
                statusView.text = getString(R.string.no_internet)
                dayView.text = getTranslatedString(currentDay)
            }
        }

        eveningButton.setOnClickListener {
            if (pageIndex == 1) {
                return@setOnClickListener
            }
            val extras = mutableMapOf<String, String>()
            if (::noonMenu.isInitialized) {
                extras["noonMenu"] = noonMenu.toJson()
            }
            if (::eveningMenu.isInitialized) {
                extras["eveningMenu"] = eveningMenu.toJson()
            }
            changePage(EveningActivity::class.java, extras)
        }


        noonButton.setOnClickListener {
            if (pageIndex == 0) {
                return@setOnClickListener
            }
            val extras = mutableMapOf<String, String>()
            if (::noonMenu.isInitialized) {
                extras["noonMenu"] = noonMenu.toJson()
            }
            if (::eveningMenu.isInitialized) {
                extras["eveningMenu"] = eveningMenu.toJson()
            }
            changePage(NoonActivity::class.java, extras)
        }

        settingsButton.setOnClickListener {
            val extras = mutableMapOf<String, String>()
            if (::noonMenu.isInitialized) {
                extras["noonMenu"] = noonMenu.toJson()
            }
            if (::eveningMenu.isInitialized) {
                extras["eveningMenu"] = eveningMenu.toJson()
            }
            changePage(SettingsActivity::class.java, extras)
        }

        dayPlusButton.setOnClickListener {
            if (currentDay != "Sunday") {
                currentDay = dayInWeek[dayInWeek.indexOf(currentDay) + 1]
                showMenu(currentDay)
            }
        }

        dayMinusButton.setOnClickListener {
            if (currentDay != "Monday") {
                currentDay = dayInWeek[dayInWeek.indexOf(currentDay) - 1]
                showMenu(currentDay)
            }
        }

        menuListView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {}
            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                menuLayout.isEnabled = firstVisibleItem == 0
            }
        })


        menuLayout.setOnRefreshListener {
            menuListView.visibility = View.GONE
            statusView.visibility = View.VISIBLE
            statusView.text = getString(R.string.loading)
            dayView.text = getString(R.string.loading_date)
            if (Request.isNetworkAvailable(this.applicationContext)) {
                try {
                    fetchMenuData()
                } catch (e: Exception) {
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.loading_error)
                }
            } else {
                menuLayout.isRefreshing = false
                statusView.text = getString(R.string.no_internet)
            }
        }
    }

    private fun changePage(page: Class<*>, extras: Map<String, String>) {
        val intent = Intent(this, page)
        for (extra in extras) {
            intent.putExtra(extra.key, extra.value)
        }
        val map = mapOf(
            "NoonActivity" to 0,
            "EveningActivity" to 1,
            "SettingsActivity" to 2
        )
        startActivity(intent).apply {
            if (pageIndex < map[page.simpleName]!!) {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }.also {
            finish()
        }
    }

    open fun fetchMenuData(): Job {
        return CoroutineScope(Dispatchers.IO).launch {}
    }

    open fun showMenu(day: String): Job {
        return CoroutineScope(Dispatchers.Main).launch {}
    }

    private fun getDayOfWeek(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> ""
        }
    }

    internal fun getFrench(day: String): String {
        return when (day) {
            "Monday" -> "Lundi"
            "Tuesday" -> "Mardi"
            "Wednesday" -> "Mercredi"
            "Thursday" -> "Jeudi"
            "Friday" -> "Vendredi"
            "Saturday" -> "Samedi"
            "Sunday" -> "Dimanche"
            else -> ""
        }
    }


    internal fun getTranslatedString(day: String): String {
        return when (day) {
            "Monday" -> getString(R.string.monday)
            "Tuesday" -> getString(R.string.tuesday)
            "Wednesday" -> getString(R.string.wednesday)
            "Thursday" -> getString(R.string.thursday)
            "Friday" -> getString(R.string.friday)
            "Saturday" -> getString(R.string.saturday)
            "Sunday" -> getString(R.string.sunday)
            else -> ""
        }
    }

}