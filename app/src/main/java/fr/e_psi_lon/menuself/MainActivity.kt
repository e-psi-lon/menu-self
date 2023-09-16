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

class MainActivity : AppCompatActivity() {
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
    private lateinit var currentPage: String

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
        currentPage = if (intent.hasExtra("currentPage")) {
            intent.getStringExtra("currentPage")!!
        } else {
            "noon"
        }

        if (File(filesDir, "config.json").exists()) {
            config = JSONObject(File(filesDir, "config.json").readText())

        } else {
            config = JSONObject()
            config.put("updateChannel", "dev")
            File(filesDir, "config.json").writeText(config.toString())
        }

        if (Request.isNetworkAvailable(this.applicationContext) && !intent.hasExtra("currentPage")) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    AutoUpdater.checkForUpdates(
                        this@MainActivity,
                        config.getString("updateChannel")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (currentPage == "noon") {
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
                    fetchMenuData(0, 2)
                } else {
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.no_internet)
                    dayView.text = getTranslatedString(currentDay)
                }
            }
        } else if (currentPage == "evening") {
            noonButton.setBackgroundColor(getColor(R.color.colorSecondaryVariant))
            eveningButton.setBackgroundColor(getColor(R.color.colorSelectedPageBackground))
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
                    fetchMenuData(2, 4)
                } else {
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.no_internet)
                    dayView.text = getTranslatedString(currentDay)
                }
            }
        }


        noonButton.setOnClickListener {
            if (currentPage != "noon") {
                val extras = mutableMapOf<String, String>()
                extras["currentPage"] = "noon"
                if (::noonMenu.isInitialized) {
                    extras["noonMenu"] = noonMenu.toJson()
                }
                if (::eveningMenu.isInitialized) {
                    extras["eveningMenu"] = eveningMenu.toJson()
                }
                changePage(MainActivity::class.java, extras)
            }
        }

        eveningButton.setOnClickListener {
            if (currentPage != "evening") {
                val extras = mutableMapOf<String, String>()
                extras["currentPage"] = "evening"
                if (::noonMenu.isInitialized) {
                    extras["noonMenu"] = noonMenu.toJson()
                }
                if (::eveningMenu.isInitialized) {
                    extras["eveningMenu"] = eveningMenu.toJson()
                }
                changePage(MainActivity::class.java, extras)
            }
        }

        settingsButton.setOnClickListener {
            val extras = mutableMapOf<String, String>()
            extras["currentPage"] = "settings"
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
                if (currentPage == "noon") {
                    fetchMenuData(0, 2)
                } else if (currentPage == "evening") {
                    fetchMenuData(2, 4)
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

    private fun fetchMenuData(start: Int, stop: Int) = CoroutineScope(Dispatchers.IO).launch {
        val doc: Document =
            Jsoup.connect("https://standarddelunivers.wordpress.com/2022/06/28/menu-de-la-semaine/")
                .get()
        val tables: MutableList<Element> = doc.select("table").toMutableList()
            .subList(start, stop)
        val days: MutableList<String> = mutableListOf()
        val contentPerDay: MutableList<MutableList<String>> =
            mutableListOf(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
        var tableI = 0
        for (table in tables) {
            for (day in table.select("th")) {
                days.add(day.text())
            }
            var perDayI = 0
            for (meal in table.select("td")) {
                if (meal.select("img").isNotEmpty()) {
                    if (meal.text() != "") {
                        val images = meal.select("img")
                        val imagesNames = mutableListOf<String>()
                        for (img in images) {
                            imagesNames.add(
                                when (img.attr("data-image-title")) {
                                    "vegetarien" -> getString(R.string.vegetarian)
                                    "pates" -> getString(R.string.home_made)
                                    else -> ""
                                }
                            )
                        }
                        contentPerDay[perDayI + tableI].add(
                            "${meal.text()} (${
                                imagesNames.joinToString(
                                    ", "
                                )
                            })"
                        )
                    }
                    continue
                }
                if (meal.text() != "") {
                    contentPerDay[perDayI + tableI].add(meal.text())
                }
                perDayI++
                if (perDayI == 2) {
                    perDayI = 0
                }
            }
            tableI++
            tableI++
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

        if (currentPage == "noon") {
            noonMenu = Menu(
                Day(days[0], contentPerDay[0]),
                Day(days[1], contentPerDay[1]),
                Day(days[2], contentPerDay[2]),
                Day(days[3], contentPerDay[3]),
                lastMenuUpdate,
                nextMenuUpdate,
                redactionMessage
            )
            println("The menu is: $noonMenu")
        } else if (currentPage == "evening") {
            eveningMenu = Menu(
                Day(days[0], contentPerDay[0]),
                Day(days[1], contentPerDay[1]),
                Day(days[2], contentPerDay[2]),
                Day(days[3], contentPerDay[3]),
                lastMenuUpdate,
                nextMenuUpdate,
                redactionMessage
            )
            println("The menu is: $eveningMenu")
        }
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

        if (day == "Monday" && currentPage == "noon") {
            dayView.text = getTranslatedString(day)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        if (day == "Friday" && currentPage == "evening") {
            dayView.text = getTranslatedString(day)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        if (!Request.isNetworkAvailable(this@MainActivity.applicationContext)) {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_internet)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        val menu = if (currentPage == "noon") {
            noonMenu
        } else {
            eveningMenu
        }
        if (menu.getDay(getFrench(day)).name == "") {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_day_data)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }

        if (menu.getDay(getFrench(day)).meals.isEmpty()) {
            dayView.text = menu.getDay(getFrench(day)).name
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, menu.getDay(getFrench(day)).name)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }

        try {
            dayView.text = menu.getDay(getFrench(day)).name
            menuListView.adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_list_item_1, menu.getDay(getFrench(day)).meals
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
