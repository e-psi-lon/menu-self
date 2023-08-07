package fr.e_psi_lon.menuself


import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    // Define navigation buttons and others variables
    private lateinit var layout: LinearLayout
    private lateinit var noonButton: ImageButton
    private lateinit var eveningButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var menuListView: ListView
    private lateinit var statusView: TextView
    private lateinit var dayPlusButton: ImageButton
    private lateinit var dayView: TextView
    private lateinit var dayMinusButton: ImageButton
    private lateinit var menu: Menu
    private val dayInWeek: List<String> = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
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

        currentPage = if (intent.hasExtra("currentPage")) {
            intent.getStringExtra("currentPage")!!
        } else {
            "noon"
        }

        if (isNetworkAvailable() && !intent.hasExtra("currentPage")) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                AddReadExternalStoragePermissions().show(supportFragmentManager, "AddReadExternalStoragePermissions")
        }
            GlobalScope.launch(Dispatchers.IO) {
                checkVersion()
            }
        }

        // Update the ListView with the menu
        if (currentPage == "noon") {
            fetchMenuData(0, 2)
        } else if (currentPage == "evening") {
            // Need to change the background color of the button
            noonButton.setBackgroundColor(ContextCompat.getColor(this,
                R.color.colorSecondaryVariant))
            eveningButton.setBackgroundColor(ContextCompat.getColor(this,
                R.color.colorSelectedPageBackground))
            settingsButton.setBackgroundColor(ContextCompat.getColor(this,
                R.color.colorSecondaryVariant))
            fetchMenuData(2, 4)
        }


        // Register click event on navigation buttons
        noonButton.setOnClickListener {
            if (currentPage != "noon") {
                changePage(MainActivity::class.java, mapOf("currentPage" to "noon"))
            }
        }

        eveningButton.setOnClickListener {
            if (currentPage != "evening") {
                changePage(MainActivity::class.java, mapOf("currentPage" to "evening"))
            }
        }

        settingsButton.setOnClickListener {
            if (currentPage != "settings") {
                changePage(SettingsActivity::class.java, mapOf("currentPage" to "settings"))
            }
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
    }

    private fun checkVersion() {
        if (AutoUpdater.getLastCommitHash() != BuildConfig.GIT_COMMIT_HASH) {
            AutoUpdater().show(supportFragmentManager, "AutoUpdater")
        }
    }

    private fun changePage(page: Class<*>, extras: Map<String, String>) {
        val intent = Intent(this, page)
        for (extra in extras) {
            intent.putExtra(extra.key, extra.value)
        }
        // We define the animation (left to right or right to left)
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

    private fun fetchMenuData(start:Int, stop: Int) = CoroutineScope(Dispatchers.IO).launch {
        val url = "https://standarddelunivers.wordpress.com/2022/06/28/menu-de-la-semaine/"
        val content = Request.get(url)
        val doc: Document = Jsoup.parse(content)
        val tables: MutableList<Element> = doc.select("table").toMutableList()
            .subList(start, stop)
        val days: MutableList<String> = tables[0].select("thead").select("tr")
            .select("th").map { it.text() }.toMutableList()
        days.addAll(tables[1].select("thead").select("tr").select("th").map { it.text() }.toMutableList())
        val day1: MutableList<String> = mutableListOf()
        val day2: MutableList<String> = mutableListOf()
        val day3: MutableList<String> = mutableListOf()
        val day4: MutableList<String> = mutableListOf()
        var rows = tables[0].select("tr")
        for (row in rows) {
            val cells = row.select("td")
            if (cells.size == 2) {
                val plat1 = cells[0].text()
                if ("<" in plat1) {
                    val platSplit = plat1.split("<")
                    day1.add(platSplit[0])
                } else if (plat1 == "" || plat1 == " ") {
                    continue
                }
                else {
                    day1.add(plat1)
                }
                val plat2 = cells[1].text()
                if ("<" in plat2) {
                    val platSplit = plat2.split("<")
                    day2.add(platSplit[0])
                } else if (plat2 == "" || plat2 == " ") {
                    continue
                }
                else {
                    day2.add(plat2)
                }
            } else if (cells.size == 1) {
                val plat1 = cells[0].text()
                if ("<" in plat1) {
                    val platSplit = plat1.split("<")
                    day1.add(platSplit[0])
                } else if (plat1 == "" || plat1 == " ") {
                    continue
                }
                else {
                    day1.add(plat1)
                }
            }
        }
        rows = tables[1].select("tr")
        for (row in rows) {
            val cells = row.select("td")
            if (cells.size == 2) {
                val plat1 = cells[0].text()
                if ("<" in plat1) {
                    val platSplit = plat1.split("<")
                    day3.add(platSplit[0])
                }
                else if (plat1=="" || plat1==" ") {
                    continue
                }
                else {
                    day3.add(plat1)
                }
                val plat2 = cells[1].text()
                if ("<" in plat2) {
                    val platSplit = plat2.split("<")
                    day4.add(platSplit[0])
                }
                else if (plat2=="" || plat2==" ") {
                    continue
                }
                else {
                    day4.add(plat2)
                }
            } else if (cells.size == 1) {
                val plat1 = cells[0].text()
                if ("<" in plat1) {
                    val platSplit = plat1.split("<")
                    day3.add(platSplit[0])
                }
                else if (plat1=="" || plat1==" ") {
                    continue
                }
                else {
                    day3.add(plat1)
                }
            }
        }
        menu = Menu(Day(days[0], day1), Day(days[1], day2), Day(days[2], day3),
            Day(days[3], day4))
        showMenu(currentDay)


    }

    private fun showMenu(day: String) = CoroutineScope(Dispatchers.Main).launch {
        if (day == "Saturday" || day == "Sunday") {
            dayView.text = day
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            return@launch
        }

        if (day == "Monday" && currentPage == "noon") {
            dayView.text = day
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            return@launch
        }
        if (day == "Friday" && currentPage == "evening") {
            dayView.text = day
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            return@launch
        }
        if (!isNetworkAvailable()) {
            dayView.text = day
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_internet)
            statusView.visibility = View.VISIBLE
            return@launch
        }
        if (menu.getDay(getFrench(day)).name == "") {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_day_data)
            statusView.visibility = View.VISIBLE
            return@launch
        }

        if (menu.getDay(getFrench(day)).meals.isEmpty()) {
            dayView.text = day
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, menu.getDay(getFrench(day)).name)
            statusView.visibility = View.VISIBLE
            return@launch
        }

        try {
            dayView.text = menu.getDay(getFrench(day)).name
            menuListView.adapter = ArrayAdapter(this@MainActivity,
                android.R.layout.simple_list_item_1, menu.getDay(getFrench(day)).meals)
            menuListView.visibility = View.VISIBLE
            statusView.visibility = View.GONE
        } catch (e: Exception) {
            statusView.text = getString(R.string.loading_error)
            statusView.visibility = View.VISIBLE
            menuListView.visibility = View.GONE
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
    private fun getFrench(day:String) : String {
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetwork
        return activeNetworkInfo != null
    }
}
