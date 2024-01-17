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
import fr.e_psi_lon.menuself.data.Day
import fr.e_psi_lon.menuself.data.Menu
import fr.e_psi_lon.menuself.others.AutoUpdater
import fr.e_psi_lon.menuself.others.capitalize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import java.util.logging.Level
import java.util.logging.Logger
import fr.e_psi_lon.menuself.data.Request as menuRequest

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
    internal var menus: MutableMap<String, Menu> = mutableMapOf()
    private lateinit var config: JSONObject
    private val dayInWeek: List<String> =
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    internal var currentDay: String = getDayOfWeek()
    internal var gotDay: Day = Day()

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.getLogger(OkHttpClient.Companion::class.java.name).level = Level.FINE
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
            currentDay = if (currentDay == "Sunday") {
                "Monday"
            } else {
                dayInWeek[dayInWeek.indexOf(currentDay) + 1]
            }
        }
        menuLayout.isRefreshing = true
        config = JSONObject(File(filesDir, "config.json").readText())
        if (config.getString("defaultActivity") == "previous") {
            config.put("previousActivity", this::class.java.simpleName)
            File(filesDir, "config.json").writeText(config.toString())
        }
        if (pageIndex == 0) {
            noonButton.setBackgroundColor(getColor(R.color.colorSelectedPageBackground))
            eveningButton.setBackgroundColor(getColor(R.color.colorSecondaryVariant))
        } else {
            eveningButton.setBackgroundColor(getColor(R.color.colorSelectedPageBackground))
            noonButton.setBackgroundColor(getColor(R.color.colorSecondaryVariant))
        }
        settingsButton.setBackgroundColor(getColor(R.color.colorSecondaryVariant))
        if (intent.hasExtra(if (pageIndex == 0) "noon" else "evening")) {
            menus[if (pageIndex == 0) "noon" else "evening"] =
                Menu.fromJson(intent.getStringExtra(if (pageIndex == 0) "noon" else "evening")!!)
            showMenu(currentDay)
            if (intent.hasExtra(if (pageIndex == 0) "evening" else "noon")) {
                menus[if (pageIndex == 0) "evening" else "noon"] =
                    Menu.fromJson(intent.getStringExtra(if (pageIndex == 0) "evening" else "noon")!!)
            }
        } else {
            if (intent.hasExtra(if (pageIndex == 0) "evening" else "noon")) {
                menus[if (pageIndex == 0) "evening" else "noon"] =
                    Menu.fromJson(intent.getStringExtra(if (pageIndex == 0) "evening" else "noon")!!)
            }
            if (menuRequest.isNetworkAvailable(this.applicationContext)) {
                try {
                    fetchMenuFromPronote()
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
            if (menus.containsKey("noon") && menus["noon"] != Menu()) {
                extras["noon"] = menus["noon"]!!.toJson()
            }
            if (menus.containsKey("evening") && menus["evening"] != Menu()) {
                extras["evening"] = menus["evening"]!!.toJson()
            }
            changePage(EveningActivity::class.java, extras)
        }


        noonButton.setOnClickListener {
            if (pageIndex == 0) {
                return@setOnClickListener
            }
            val extras = mutableMapOf<String, String>()
            if (menus.containsKey("noon") && menus["noon"] != Menu()) {
                extras["noon"] = menus["noon"]!!.toJson()
            }
            if (menus.containsKey("evening") && menus["evening"] != Menu()) {
                extras["evening"] = menus["evening"]!!.toJson()
            }
            changePage(NoonActivity::class.java, extras)
        }

        settingsButton.setOnClickListener {
            val extras = mutableMapOf<String, String>()
            if (menus.containsKey("noon") && menus["noon"] != Menu()) {
                extras["noon"] = menus["noon"]!!.toJson()
            }
            if (menus.containsKey("evening") && menus["evening"] != Menu()) {
                extras["evening"] = menus["evening"]!!.toJson()
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
            if (menuRequest.isNetworkAvailable(this.applicationContext)) {
                try {
                    menus["evening"] = Menu()
                    menus["noon"] = Menu()
                    fetchMenuFromPronote(true)
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

    @OptIn(DelicateCoroutinesApi::class)
    fun checkForUpdates() {
        if (menuRequest.isNetworkAvailable(this.applicationContext) && intent.hasExtra("init")) {
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


    private fun getMeal(mealToParse: JSONArray): MutableList<String> {
        val meals = mutableListOf<String>()
        for (i in 0 until mealToParse.length()) {
            val mealObject = mealToParse.getJSONObject(i)
            val labels = mealObject.getJSONArray("labels")
            var meal = mealObject.getString("name")
            meal = meal.capitalize()
            val labelsString = mutableListOf<String>()
            if (labels.length() > 0) {
                for (j in 0 until labels.length()) {
                    @Suppress("SpellCheckingInspection")
                    val translated = when (labels.getJSONObject(j).getString("name")) {
                        "Végétarien" -> getString(R.string.vegetarian)
                        "Fait maison" -> getString(R.string.home_made)
                        "Assemblé sur place" -> getString(R.string.assembled_on_site)
                        "Haute valeur environnementale" -> getString(R.string.high_environmental_value)
                        "Issu de l'Agriculture Biologique" -> getString(R.string.organic)
                        "Fait maison - Recette du chef" -> getString(R.string.home_made_chef_recipe)
                        "Produits locaux" -> getString(R.string.local_products)
                        else -> labels.getJSONObject(j).getString("name").capitalize()
                    }
                    labelsString.add(translated)
                }
                if (labelsString.size == 1) {
                    meals.add("$meal (${labelsString[0]})")
                    continue
                }
                if (labelsString.size == 2) {
                    meals.add("$meal (${labelsString[0]} ${getString(R.string.and)} ${labelsString[1]})")
                    continue
                }
                val lastLabel = labelsString[labelsString.size - 1]
                labelsString.removeAt(labelsString.size - 1)
                meals.add("$meal (${labelsString.joinToString(", ")} ${getString(R.string.and)} $lastLabel")
            } else {
                meals.add(meal)
            }
        }
        return meals
    }


    private fun parseMeal(mealData: JSONObject): MutableList<String> {
        val meals = mutableListOf<String>()

        fun addMealFromKey(key: String) {
            if (mealData.has(key) && !mealData.isNull(key)) {
                if (meals.isNotEmpty()) {
                    meals.add("~~")
                }
                meals.addAll(getMeal(mealData.getJSONArray(key)))
            }
        }
        for (key in listOf(
            "first_meal",
            "main_meal",
            "side_meal",
            "other_meal",
            "cheese",
            "dessert"
        )) {
            addMealFromKey(key)
        }
        return meals
    }

    private fun getOrCheckToken(config: JSONObject): String {
        if (config.has("usePronote") && !config.getBoolean("usePronote")) {
            return ""
        }
        if (config.has("pronoteToken")) {
            val token = config.getString("pronoteToken")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("${config.get("pronoteAPI")}/user?token=$token")
                .build()
            val response = client.newCall(request).execute()
            return if (response.code == 200) {
                token
            } else {
                config.remove("pronoteToken")
                File(filesDir, "config.json").writeText(config.toString())
                getOrCheckToken(config)
            }
        } else {
            val username = config.getString("pronoteUsername")
            val password = config.getString("pronotePassword")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("${config.get("pronoteAPI")}/generatetoken")
                .post(
                    JSONObject(
                        mapOf(
                            "username" to username,
                            "password" to password,
                            "url" to "https://0410002e.index-education.net/pronote/eleve.html",
                            "ent" to "ac_orleans_tours"
                        )
                    )
                        .toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()
            val response = client.newCall(request).execute()
            return if (response.code == 200) {
                val token = JSONObject(response.body!!.string()).getString("token")
                config.put("pronoteToken", token)
                File(filesDir, "config.json").writeText(config.toString())
                token
            } else {
                ""
            }
        }
    }

    private fun fetchMenuFromPronote(onReload: Boolean = false) = CoroutineScope(Dispatchers.IO).launch {
        val menusFile = File(applicationContext.filesDir, "menus.json")
        val json = if (menusFile.exists()) {
            JSONObject(menusFile.readText())
        } else {
            JSONObject()
        }
        if (json.toString() != "{}" && !onReload) {
            val today = Calendar.getInstance()
            val date = json.getString("date")
            val todayString =
                "${today.get(Calendar.DAY_OF_MONTH)}-${today.get(Calendar.MONTH)}-${
                    today.get(
                        Calendar.YEAR
                    )
                }"
            if (date == todayString) {
                if (json.has("noon_menu")) {
                    menus["noon"] = Menu.fromJson(json.getString("noon_menu"))
                }
                if (json.has("evening_menu")) {
                    menus["evening"] = Menu.fromJson(json.getString("evening_menu"))
                }
            }
            if (pageIndex == 0 && menus.containsKey("noon") && menus["noon"] != Menu()) {
                showMenu(currentDay)
                checkForUpdates()
                return@launch
            }
            if (pageIndex == 1 && menus.containsKey("evening") && menus["evening"] != Menu()) {
                showMenu(currentDay)
                checkForUpdates()
                return@launch
            }
        }
        val client = OkHttpClient()
        if (!config.has("pronoteUsername") || !config.has("pronotePassword")) {
            try {
                menus["evening"] = Menu()
                menus["noon"] = Menu()
                fetchMenuData()
            } catch (e: Exception) {
                menuLayout.isRefreshing = false
                statusView.text = getString(R.string.loading_error)
            }
            return@launch
        }
        try {
            val token = getOrCheckToken(config)
            if (token == "") {
                try {
                    menus["evening"] = Menu()
                    menus["noon"] = Menu()
                    fetchMenuData()
                } catch (e: Exception) {
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.loading_error)
                }
                return@launch
            }
            config.put("pronoteToken", token)
            File(filesDir, "config.json").writeText(config.toString())
            val dateFrom = Calendar.getInstance().let {
                if (it.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    it.add(Calendar.DAY_OF_YEAR, 2)
                } else if (it.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    it.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (it.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    it.add(Calendar.DAY_OF_YEAR, 2 - it.get(Calendar.DAY_OF_WEEK))
                }
                "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH) + 1}-${it.get(Calendar.DAY_OF_MONTH)}"
            }
            val dateFromAsCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateFrom.split("-")[0].toInt())
                set(Calendar.MONTH, dateFrom.split("-")[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, dateFrom.split("-")[2].toInt())
            }
            val dateTo = dateFromAsCalendar.apply {
                add(Calendar.DAY_OF_YEAR, 5)
            }.let {
                "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH) + 1}-${it.get(Calendar.DAY_OF_MONTH)}"
            }
            val request = Request.Builder()
                .url("${config.get("pronoteAPI")}/menu?token=$token&dateFrom=$dateFrom&dateTo=$dateTo")
                .build()
            val pronoteMenu = client.newCall(request).execute().body?.string() ?: ""
            @Suppress("SpellCheckingInspection")
            if (pronoteMenu == "" || pronoteMenu == "\"notfound\"" || pronoteMenu == "[]") {
                try {
                    menus["evening"] = Menu()
                    menus["noon"] = Menu()
                    fetchMenuData()
                } catch (e: Exception) {
                    menuLayout.isRefreshing = false
                    statusView.text = getString(R.string.loading_error)
                }
                return@launch
            }
            val pronoteMenuJson = JSONArray(pronoteMenu)
            val lunchDays = mutableListOf<Day>()
            val dinnerDays = mutableListOf<Day>()
            for (i in 0 until pronoteMenuJson.length()) {
                val meals = mutableListOf<String>()
                val meals2 = mutableListOf<String>()
                if (pronoteMenuJson.getJSONObject(i).getJSONObject("type").getBoolean("is_lunch")) {
                    meals.addAll(
                        parseMeal(
                            pronoteMenuJson.getJSONObject(i)
                        )
                    )
                } else if (pronoteMenuJson.getJSONObject(i).getJSONObject("type")
                        .getBoolean("is_dinner")
                ) {
                    meals2.addAll(
                        parseMeal(
                            pronoteMenuJson.getJSONObject(i)
                        )
                    )
                }
                val calendar = Calendar.getInstance().apply {
                    set(
                        pronoteMenuJson.getJSONObject(i).getString("date").split("-")[0].toInt(),
                        pronoteMenuJson.getJSONObject(i).getString("date")
                            .split("-")[1].toInt() - 1,
                        pronoteMenuJson.getJSONObject(i).getString("date").split("-")[2].toInt()
                    )
                }
                val formattedDate =
                    "${getFrench(dayInWeek[calendar.get(Calendar.DAY_OF_WEEK) - 2])} ${
                        pronoteMenuJson.getJSONObject(i).getString("date").split("-")[2]
                    }/${
                        pronoteMenuJson.getJSONObject(i).getString("date").split("-")[1]
                    }/${pronoteMenuJson.getJSONObject(i).getString("date").split("-")[0]}"

                lunchDays.add(
                    Day(
                        formattedDate, meals, mapOf(
                            "year" to pronoteMenuJson.getJSONObject(i).getString("date")
                                .split("-")[0].toInt(),
                            "month" to pronoteMenuJson.getJSONObject(i).getString("date")
                                .split("-")[1].toInt(),
                            "day" to pronoteMenuJson.getJSONObject(i).getString("date")
                                .split("-")[2].toInt()
                        )
                    )
                )

                dinnerDays.add(
                    Day(
                        formattedDate, meals2, mapOf(
                            "year" to pronoteMenuJson.getJSONObject(i).getString("date")
                                .split("-")[0].toInt(),
                            "month" to pronoteMenuJson.getJSONObject(i).getString("date")
                                .split("-")[1].toInt(),
                            "day" to pronoteMenuJson.getJSONObject(i).getString("date")
                                .split("-")[2].toInt()
                        )
                    )
                )
            }

            if (listOf(4, 5).contains(lunchDays.size)) {
                menus["noon"] = if (lunchDays.size == 4) {
                    Menu(
                        lunchDays[0],
                        lunchDays[1],
                        lunchDays[2],
                        lunchDays[3],
                        getString(R.string.no_when_pronote),
                        getString(R.string.no_when_pronote),
                        getString(R.string.no_when_pronote)
                    )
                } else {
                    Menu(
                        lunchDays[0],
                        lunchDays[1],
                        lunchDays[2],
                        lunchDays[3],
                        getString(R.string.no_when_pronote),
                        getString(R.string.no_when_pronote),
                        getString(R.string.no_when_pronote),
                        lunchDays[4]
                    )
                }
            } else {
                if (pageIndex == 0) {
                    menus["noon"] = Menu()
                    fetchMenuData()
                } else {
                    menus["noon"] = Menu()
                }
            }
            if (dinnerDays.size == 4) {
                menus["evening"] = Menu(
                    dinnerDays[0],
                    dinnerDays[1],
                    dinnerDays[2],
                    dinnerDays[3],
                    getString(R.string.no_when_pronote),
                    getString(R.string.no_when_pronote),
                    getString(R.string.no_when_pronote)
                )
            } else {
                if (pageIndex == 1) {
                    menus["evening"] = Menu()
                    fetchMenuData()
                } else {
                    menus["evening"] = Menu()
                }
            }
            showMenu(currentDay)
            if (!onReload) {
                checkForUpdates()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                menus["evening"] = Menu()
                menus["noon"] = Menu()
                fetchMenuData()
            } catch (e: Exception) {
                menuLayout.isRefreshing = false
                statusView.text = getString(R.string.loading_error)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        finishAffinity()
    }

    open fun fetchMenuData(specificDay: String = "", onReload: Boolean = false): Job {
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