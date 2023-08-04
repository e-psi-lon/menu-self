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
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    // Déclarer les boutons de navigation
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
    private lateinit var cachedMenu: Menu
    private val dayInWeek: List<String> = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
    private var currentDay: String = getDayOfWeek()
    var currentPage = "noon"

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

        // Mettre à jour la ListView avec les menus
        println("Fetching menu data...")
        fetchMenuData(0, 2)

        // Gérer les clics sur les boutons de navigation
        noonButton.setOnClickListener {
            if (currentPage != "noon") {
                // Faire quelque chose lorsque le bouton "Menu du noon" est cliqué
                currentPage = "noon"
                noonButton.setBackgroundColor(ContextCompat.getColor(this,
                    R.color.colorSelectedPageBackground))
                eveningButton.setBackgroundColor(ContextCompat.getColor(this,
                    R.color.colorSecondaryVariant))
                settingsButton.setBackgroundColor(ContextCompat.getColor(this,
                    R.color.colorSecondaryVariant))
                if (::cachedMenu.isInitialized) {
                    menu = cachedMenu
                    showMenu(currentDay)
                } else {
                    cachedMenu = menu
                    fetchMenuData(0, 2)
                }
            }
        }

        eveningButton.setOnClickListener {
            if (currentPage != "evening") {
                currentPage = "evening"
                noonButton.setBackgroundColor(ContextCompat.getColor(this,
                    R.color.colorSecondaryVariant))
                eveningButton.setBackgroundColor(ContextCompat.getColor(this,
                    R.color.colorSelectedPageBackground))
                settingsButton.setBackgroundColor(ContextCompat.getColor(this,
                    R.color.colorSecondaryVariant))
                if (::cachedMenu.isInitialized) {
                    menu = cachedMenu
                    showMenu(currentDay)
                } else {
                    cachedMenu = menu
                    fetchMenuData(2, 4)
                }
            }

        }

        settingsButton.setOnClickListener {
            // Faire quelque chose lorsque le bouton "Paramètres" est cliqué
            if (currentPage != "settings") {
                currentPage = "settings"
                val intent = Intent(this, SettingsActivity::class.java)
                // We start the activity
                startActivity(intent)

            }

        }

        dayPlusButton.setOnClickListener {
            // Faire quelque chose lorsque le bouton "Jour suivant" est cliqué
            if (currentDay != "Sunday") {
                currentDay = dayInWeek[dayInWeek.indexOf(currentDay) + 1]
                showMenu(currentDay)
            }
        }

        dayMinusButton.setOnClickListener {
            // Faire quelque chose lorsque le bouton "Jour précédent" est cliqué
            if (currentDay != "Monday") {
                currentDay = dayInWeek[dayInWeek.indexOf(currentDay) - 1]
                showMenu(currentDay)
            }
        }
    }

    private fun fetchMenuData(start:Int, stop: Int) = CoroutineScope(Dispatchers.IO).launch {
        // Remplacer cette partie par l'appel réel à l'API pour récupérer les menus
        val url = "https://standarddelunivers.wordpress.com/2022/06/28/menu-de-la-semaine/"
        println("Fetching $url...")
        val doc: Document = Jsoup.connect(url).get()
        // On selectionne les deux derniers tableaux (3 et 4) qui correspondent au menu du soir
        val tables: MutableList<Element> = doc.select("table").toMutableList()
            .subList(start, stop)
        // On récupère les jours de la semaine
        val days: MutableList<String> = tables[0].select("thead").select("tr")
            .select("th").map { it.text() }.toMutableList()
        days.addAll(tables[1].select("thead").select("tr").select("th").map { it.text() }.toMutableList())
        println("Days")
        println(days)
        // On récupère les colonnes de chaque jour
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
                    println(plat1)
                    val platSplit = plat1.split("<")
                    day1.add(platSplit[0])
                    println("Text added to day1 is ${platSplit[0]} (now day1 is $day1)")
                } else if (plat1 == "" || plat1 == " ") {
                    continue
                }
                else {
                    day1.add(plat1)
                    println("Text added to day1 is $plat1 (now day1 is $day1)")
                }
                val plat2 = cells[1].text()
                if ("<" in plat2) {
                    println(plat2)
                    val platSplit = plat2.split("<")
                    day2.add(platSplit[0])
                    println("Text added to day2 is ${platSplit[0]} (now day2 is $day2)")
                } else if (plat2 == "" || plat2 == " ") {
                    continue
                }
                else {
                    day2.add(plat2)
                    println("Text added to day2 is $plat2 (now day2 is $day2)")
                }
            } else if (cells.size == 1) {
                val plat1 = cells[0].text()
                if ("<" in plat1) {
                    println(plat1)
                    val platSplit = plat1.split("<")
                    day1.add(platSplit[0])
                    println("Text added to day1 is ${platSplit[0]} (now day1 is $day1)")
                } else if (plat1 == "" || plat1 == " ") {
                    continue
                }
                else {
                    day1.add(plat1)
                    println("Text added to day1 is $plat1 (now day1 is $day1)")
                }
            }
        }
        rows = tables[1].select("tr")
        for (row in rows) {
            val cells = row.select("td")
            if (cells.size == 2) {
                val plat1 = cells[0].text()
                if ("<" in plat1) {
                    println(plat1)
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
                    println(plat2)
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
                    println(plat1)
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
        println("Menu object is $menu")
        showMenu(currentDay)


    }

    private fun showMenu(day: String) = CoroutineScope(Dispatchers.Main).launch {
        // Mettre à jour la ListView avec les menus
        if (day == "Saturday" || day == "Sunday" || day == "Monday") {
            dayView.text = day
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            return@launch
        }
        // We check if there is an Internet connection
        if (!isNetworkAvailable()) {
            dayView.text = day
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_internet)
            statusView.visibility = View.VISIBLE
            return@launch
        }
        try {
            println("Trying to change date for $day")
            dayView.text = menu.getDay(getFrench(day)).name
            println("Changing date for $day")
            menuListView.adapter = ArrayAdapter(this@MainActivity,
                android.R.layout.simple_list_item_1, menu.getDay(getFrench(day)).meals)
            println("Setting adapter with ${menu.getDay(getFrench(day)).meals}")
            menuListView.visibility = View.VISIBLE
            println("Setting menuListView to visible")
            statusView.visibility = View.GONE
            println("Setting statusView to gone")
        } catch (e: Exception) {
            println("Exception: $e")
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
