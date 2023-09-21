package fr.e_psi_lon.menuself

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.util.Calendar

class NoonActivity : AppCompatActivity() {
    private lateinit var layout: LinearLayout
    private lateinit var noonButton: ImageButton
    private lateinit var eveningButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var menuListView: ListView
    private lateinit var statusView: TextView
    private lateinit var dayPlusButton: ImageButton
    private lateinit var dayView: TextView
    private lateinit var dayMinusButton: ImageButton
    private lateinit var menuLayout: SwipeRefreshLayout
    private lateinit var eveningMenu: Menu
    private lateinit var noonMenu: Menu
    private lateinit var config: JSONObject
    private val dayInWeek: List<String> =
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private var currentDay: String = getDayOfWeek()

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

        menuLayout.isRefreshing = true
        config = JSONObject(File(filesDir, "config.json").readText())
        if (config.getString("defaultActivity") == "previous") {
            config.put("previousActivity", "NoonActivity")
            File(filesDir, "config.json").writeText(config.toString())
        }
        if (Request.isNetworkAvailable(this.applicationContext) && intent.hasExtra("init")) {
            println("We have internet and init")
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    AutoUpdater.checkForUpdates(
                        this@NoonActivity,
                        config.getString("updateChannel")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (intent.hasExtra("noonMenu")) {
            noonMenu = Menu.fromJson(intent.getStringExtra("noonMenu")!!)
            showMenu(currentDay)
            if (intent.hasExtra("eveningMenu")) {
                eveningMenu = Menu.fromJson(intent.getStringExtra("eveningMenu")!!)
            }
        } else {
            if (intent.hasExtra("eveningMenu")) {
                eveningMenu = Menu.fromJson(intent.getStringExtra("eveningMenu")!!)
            }
            if (Request.isNetworkAvailable(this.applicationContext)) {
                try {
                    fetchMenuData()
                } catch (e: Exception) {
                    e.printStackTrace()
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.loading_error)
                    dayView.text = getTranslatedString(currentDay)
                }
            } else {
                menuLayout.isRefreshing = false
                statusView.text = getString(R.string.no_internet)
                dayView.text = getTranslatedString(currentDay)
            }
        }

        eveningButton.setOnClickListener {
            val extras = mutableMapOf<String, String>()
            if (::noonMenu.isInitialized) {
                extras["noonMenu"] = noonMenu.toJson()
            }
            if (::eveningMenu.isInitialized) {
                extras["eveningMenu"] = eveningMenu.toJson()
            }
            changePage(EveningActivity::class.java, extras)
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
                    e.printStackTrace()
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.loading_error)
                    dayView.text = getTranslatedString(currentDay)
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
        val index = 0
        startActivity(intent).apply {
            if (index < map[page.simpleName]!!) {
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

    private fun fetchMenuData() = CoroutineScope(Dispatchers.IO).launch {
        val doc: Document =
            Jsoup.connect("https://standarddelunivers.wordpress.com/2022/06/28/menu-de-la-semaine/")
                .get()
        val tables: MutableList<Element> = doc.select("table").toMutableList()
            .subList(0, 3)
        val days: MutableList<String> = mutableListOf()
        val contentPerDay: MutableList<MutableList<String>> =
            mutableListOf(
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            )
        for (table in tables) {
            for (th in table.select("th")) {
                days.add(th.text())
            }
        }
        for (td in tables[0].select("td")) {
            if (td.text() != "") {
                if (td.select("img").isNotEmpty()) {
                    val images = td.select("img")
                    val imagesNames = mutableListOf<String>()
                    for (img in images) {
                        imagesNames.add(
                            when (img.attr("data-image-title")) {
                                "vegetarien" -> getString(R.string.vegetarian)
                                "pates" -> getString(R.string.home_made)
                                else -> img.attr("data-image-title")
                            }
                        )
                    }
                    contentPerDay[0].add("${td.text()} (${imagesNames.joinToString(", ")})")
                } else {
                    contentPerDay[0].add(td.text())
                }
            }
        }
        for ((index, td) in tables[1].select("td").withIndex()) {
            if (index % 2 == 0) {
                if (td.text() != "") {
                    if (td.select("img").isNotEmpty()) {
                        val images = td.select("img")
                        val imagesNames = mutableListOf<String>()
                        for (img in images) {
                            imagesNames.add(
                                when (img.attr("data-image-title")) {
                                    "vegetarien" -> getString(R.string.vegetarian)
                                    "pates" -> getString(R.string.home_made)
                                    else -> img.attr("data-image-title")
                                }
                            )
                        }
                        contentPerDay[1].add("${td.text()} (${imagesNames.joinToString(", ")})")
                    } else {
                        contentPerDay[1].add(td.text())
                    }
                }
            } else {
                if (td.text() != "") {
                    if (td.select("img").isNotEmpty()) {
                        val images = td.select("img")
                        val imagesNames = mutableListOf<String>()
                        for (img in images) {
                            imagesNames.add(
                                when (img.attr("data-image-title")) {
                                    "vegetarien" -> getString(R.string.vegetarian)
                                    "pates" -> getString(R.string.home_made)
                                    else -> img.attr("data-image-title")
                                }
                            )
                        }
                        contentPerDay[2].add("${td.text()} (${imagesNames.joinToString(", ")})")
                    } else {
                        contentPerDay[2].add(td.text())
                    }
                }
            }
        }
        for ((index, td) in tables[2].select("td").withIndex()) {
            if (index % 2 == 0) {
                if (td.text() != "") {
                    if (td.select("img").isNotEmpty()) {
                        val images = td.select("img")
                        val imagesNames = mutableListOf<String>()
                        for (img in images) {
                            imagesNames.add(
                                when (img.attr("data-image-title")) {
                                    "vegetarien" -> getString(R.string.vegetarian)
                                    "pates" -> getString(R.string.home_made)
                                    else -> img.attr("data-image-title")
                                }
                            )
                        }
                        contentPerDay[3].add("${td.text()} (${imagesNames.joinToString(", ")})")
                    } else {
                        contentPerDay[3].add(td.text())
                    }
                }
            } else {
                if (td.text() != "") {
                    if (td.select("img").isNotEmpty()) {
                        val images = td.select("img")
                        val imagesNames = mutableListOf<String>()
                        for (img in images) {
                            imagesNames.add(
                                when (img.attr("data-image-title")) {
                                    "vegetarien" -> getString(R.string.vegetarian)
                                    "pates" -> getString(R.string.home_made)
                                    else -> img.attr("data-image-title")
                                }
                            )
                        }
                        contentPerDay[4].add("${td.text()} (${imagesNames.joinToString(", ")})")
                    } else {
                        contentPerDay[4].add(td.text())
                    }
                }
            }
        }
        var lastMenuUpdate = ""
        var nextMenuUpdate = ""
        var redactionMessage: String? = null
        for (p in doc.select("p")) {
            if (p.text().contains("Dernière mise à jour")) {
                lastMenuUpdate = p.text().replace("Dernière mise à jour : ", "")
            }
            if (p.text().contains("Prochaine mise à jour")) {
                nextMenuUpdate = p.text().replace("Prochaine mise à jour : ", "")
            }
            if (p.text() == "La Rédaction") {
                redactionMessage = p.previousElementSibling()?.text()
            }
        }


        noonMenu = Menu(
            Day(days[0], contentPerDay[0]),
            Day(days[1], contentPerDay[1]),
            Day(days[2], contentPerDay[2]),
            Day(days[3], contentPerDay[3]),
            lastMenuUpdate,
            nextMenuUpdate,
            redactionMessage,
            Day(days[4], contentPerDay[4])
        )
        showMenu(currentDay)


    }

    private fun showMenu(day: String) = CoroutineScope(Dispatchers.Main).launch {
        if (day == "Saturday" || day == "Sunday") {
            dayView.text = getTranslatedString(day)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        if (!Request.isNetworkAvailable(this@NoonActivity.applicationContext)) {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_internet)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        if (noonMenu.getDay(getFrench(day)).name == "") {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_day_data)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }

        if (noonMenu.getDay(getFrench(day)).meals.isEmpty()) {
            dayView.text = noonMenu.getDay(getFrench(day)).name
            menuListView.visibility = View.GONE
            statusView.text =
                getString(R.string.no_menu_this_day, noonMenu.getDay(getFrench(day)).name)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }

        try {
            dayView.text = noonMenu.getDay(getFrench(day)).name
            menuListView.adapter = ArrayAdapter(
                this@NoonActivity,
                android.R.layout.simple_list_item_1, noonMenu.getDay(getFrench(day)).meals
            )
            menuListView.visibility = View.VISIBLE
            menuListView.isEnabled = true
            statusView.visibility = View.GONE
        } catch (e: Exception) {
            statusView.text = getString(R.string.loading_error)
            statusView.visibility = View.VISIBLE
            menuListView.visibility = View.GONE
        } finally {
            menuLayout.isRefreshing = false
        }
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

    private fun getFrench(day: String): String {
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


    private fun getTranslatedString(day: String): String {
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