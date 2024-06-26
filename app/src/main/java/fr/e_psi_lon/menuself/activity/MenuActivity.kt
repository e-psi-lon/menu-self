package fr.e_psi_lon.menuself.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import fr.e_psi_lon.menuself.R
import fr.e_psi_lon.menuself.data.Day
import fr.e_psi_lon.menuself.data.Menu
import fr.e_psi_lon.menuself.others.AutoUpdater
import fr.e_psi_lon.menuself.others.capitalize
import kotlinx.coroutines.*

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
    private lateinit var menuListView: ListView
    private lateinit var statusView: TextView
    private lateinit var dayPlusButton: ImageButton
    private lateinit var dayView: TextView
    private lateinit var dayMinusButton: ImageButton
    internal lateinit var menuLayout: SwipeRefreshLayout
    private var menus: MutableMap<String, Menu> = mutableMapOf()
    private lateinit var config: JSONObject
    private val dayInWeek: List<String> =
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private var currentDay: String = getDayOfWeek()

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.getLogger(OkHttpClient.Companion::class.java.name).level = Level.FINE
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        layout = findViewById(R.id.main_layout)
        noonButton = findViewById(R.id.noon_button)
        eveningButton = findViewById(R.id.evening_button)
        settingsButton = findViewById(R.id.settings_button)
        menuListView = findViewById(R.id.menu_list_view)
        dayPlusButton = findViewById(R.id.next_button)
        dayMinusButton = findViewById(R.id.previous_button)
        dayView = findViewById(R.id.date_text_view)
        statusView = findViewById(R.id.status_text_view)
        menuLayout = findViewById(R.id.meal_layout)
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
            noonButton.setBackgroundColor(getColor(R.color.color_selected_page_background))
            eveningButton.setBackgroundColor(getColor(R.color.color_secondary_variant))
        } else {
            eveningButton.setBackgroundColor(getColor(R.color.color_selected_page_background))
            noonButton.setBackgroundColor(getColor(R.color.color_secondary_variant))
        }
        settingsButton.setBackgroundColor(getColor(R.color.color_secondary_variant))
        if (intent.hasExtra(if (pageIndex == 0) "noon" else "evening")) {
            menus[if (pageIndex == 0) "noon" else "evening"] =
                Menu.fromJson(intent.getStringExtra(if (pageIndex == 0) "noon" else "evening")!!)
            CoroutineScope(Dispatchers.Main).launch {
                showMenu(currentDay)
            }
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
                    CoroutineScope(Dispatchers.IO).launch {
                        fetchMenuFromPronote()
                    }
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
                    CoroutineScope(Dispatchers.IO).launch {
                        fetchMenuFromPronote(true)
                    }
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

            @Suppress("SpellCheckingInspection")
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
                val token = JSONObject(response.body.string()).getString("token")
                config.put("pronoteToken", token)
                File(filesDir, "config.json").writeText(config.toString())
                token
            } else {
                ""
            }
        }
    }

    private suspend fun fetchMenuFromPronote(onReload: Boolean = false) {
            val menusFile = File(applicationContext.filesDir, "menus.json")
            val json = if (menusFile.exists()) {
                try {
                    JSONObject(menusFile.readText())
                } catch (e: Exception) {
                    menusFile.delete()
                    JSONObject()
                }
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
                    withContext(Dispatchers.Main) {
                        showMenu(currentDay)
                        checkForUpdates()
                    }
                    return
                }
                if (pageIndex == 1 && menus.containsKey("evening") && menus["evening"] != Menu()) {
                    withContext(Dispatchers.Main) {
                        showMenu(currentDay)
                        checkForUpdates()
                    }
                    return
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
                return
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
                    return
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
                val pronoteMenu = client.newCall(request).execute().body.string()
                if (pronoteMenu == "" || pronoteMenu == "\"notfound\"" || pronoteMenu == "[]") {
                    try {
                        menus["evening"] = Menu()
                        menus["noon"] = Menu()
                        fetchMenuData()
                    } catch (e: Exception) {
                        menuLayout.isRefreshing = false
                        statusView.text = getString(R.string.loading_error)
                    }
                    return
                }
                val pronoteMenuJson = JSONArray(pronoteMenu)
                val lunchDays = mutableListOf<Day>()
                val dinnerDays = mutableListOf<Day>()
                for (i in 0 until pronoteMenuJson.length()) {
                    val meals = mutableListOf<String>()
                    val meals2 = mutableListOf<String>()
                    if (pronoteMenuJson.getJSONObject(i).getJSONObject("type")
                            .getBoolean("is_lunch")
                    ) {
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
                            pronoteMenuJson.getJSONObject(i).getString("date")
                                .split("-")[0].toInt(),
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
                        )
                    } else {
                        Menu(
                            lunchDays[0],
                            lunchDays[1],
                            lunchDays[2],
                            lunchDays[3],
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
                withContext(Dispatchers.Main) {
                    showMenu(currentDay)
                }
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

    private fun showMenu(day: String) {
        if (day == "Saturday" || day == "Sunday") {
            dayView.text = getTranslatedString(day)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return
        }
        if (!fr.e_psi_lon.menuself.data.Request.isNetworkAvailable(this.applicationContext)) {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_internet)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return
        }
        if (menus[if (pageIndex == 0) "noon" else "evening"]?.getDay(getFrench(day))?.name == "") {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_day_data)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return
        }

        if (menus[if (pageIndex == 0) "noon" else "evening"]?.getDay(getFrench(day))?.meals?.isEmpty() == true) {
            dayView.text = menus[if (pageIndex == 0) "noon" else "evening"]?.getDay(getFrench(day))?.name ?: ""
            menuListView.visibility = View.GONE
            statusView.text =
                getString(
                    R.string.no_menu_this_day,
                    menus[if (pageIndex == 0) "noon" else "evening"]?.getDay(getFrench(day))?.name ?: ""
                )
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return
        }

        try {
            dayView.text = menus[if (pageIndex == 0) "noon" else "evening"]?.getDay(getFrench(day))?.name
            menuListView.adapter = menus[if (pageIndex == 0) "noon" else "evening"]?.getDay(getFrench(day))?.let {
                ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1, it.meals
                )
            }
            menuListView.visibility = View.VISIBLE
            menuListView.isEnabled = true
            statusView.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()

            statusView.text = getString(R.string.loading_error)
            statusView.visibility = View.VISIBLE
            menuListView.visibility = View.GONE
        } finally {
            menuLayout.isRefreshing = false
        }

        val eveningMenu = menus["evening"]?.toJson()
        val noonMenu = menus["noon"]?.toJson()
        val calendar = Calendar.getInstance()
        val date = "${calendar.get(Calendar.DAY_OF_MONTH)}-${calendar.get(Calendar.MONTH)}-${
            calendar.get(
                Calendar.YEAR
            )
        }_${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${
            calendar.get(
                Calendar.SECOND
            )
        }"
        val file = File(
            applicationContext.filesDir,
            "menus.json"
        )
        if (!file.exists()) {
            CoroutineScope(Dispatchers.IO).launch {
                file.createNewFile()
            }
        }
        val json =
            "{\"date\":\"$date\"${if (eveningMenu != null) ",\"evening_menu\":$eveningMenu" else ""}${if (noonMenu != null) ",\"noon_menu\":$noonMenu" else ""}}"
        file.writeText(json)
    }

    private suspend fun fetchMenuData(onReload: Boolean = false) {
        if (menus[if (pageIndex == 0) "noon" else "evening"] != Menu()) {
            withContext(Dispatchers.Main) {
                showMenu(currentDay)
            }
            return
        }
        val doc: Document = if (pageIndex == 0)
            Jsoup.connect("https://filtreimages.neocities.org/menu-de-la-semaine")
                .get() else Jsoup.connect("https://filtreimages.neocities.org/menu-soir").get()
        val script = doc.select("body").select("script")[0].data()
        val days = mutableListOf<String>()
        val contentPerDay = mutableListOf<List<String>>()
        val tableHeadersRegex =
            Regex("var tableHeaders = (\\{[^}]+\\});", RegexOption.MULTILINE)
        val tableHeaders = tableHeadersRegex.find(script)?.groupValues?.get(1)
            ?.replace(Regex("""(\d+):"""), """'$1':""")
            ?.trimIndent()?.replace("\n", "")?.replace("  ", "")
            ?.trim()?.replace(",}", "}")
        val tableHeadersJson = tableHeaders?.let { JSONObject(it) }
        for (i in 0 until tableHeadersJson?.length()!!) {
            val date = tableHeadersJson.getString((i + 1).toString()).replace(
                "Menu de ",
                ""
            ).split(" ")
            val day = date[0]
            val dateStr = date.subList(1, date.size).joinToString("/")
                .replace("Janvier", "01").replace("Février", "02")
                .replace("Mars", "03").replace("Avril", "04").replace("Mai", "05")
                .replace("Juin", "06").replace("Juillet", "07").replace("Août", "08")
                .replace("Septembre", "09").replace("Octobre", "10").replace("Novembre", "11")
                .replace("Décembre", "12").replace("1er", "1")
            days.add("$day $dateStr")
        }
        val tableDataRegex = Regex("var tableData = (\\{[^}]+\\});", RegexOption.MULTILINE)
        val tableData = tableDataRegex.find(script)?.groupValues?.get(1)
            ?.replace(Regex("""(\d+):"""), """'$1':""")
            ?.trimIndent()?.replace("\n", "")?.replace("  ", "")
            ?.trim()?.replace(",}", "}")
        val tableDataJson = tableData?.let { JSONObject(it) }
        for (i in 0 until tableDataJson?.length()!!) {
            val content = mutableListOf<String>()
            for (j in 0 until tableDataJson.getJSONArray((i + 1).toString()).length()) {
                val element = tableDataJson.getJSONArray((i + 1).toString()).getString(j)
                val added = mutableListOf<String>()
                if (element.contains("<br>~~<br>")) {
                    element.split("<br>~~<br>").map {
                        var tempString = it
                        val tags = mutableListOf<String>()
                        if ("🍃" in it) {
                            tags.add(getString(R.string.vegetarian))
                            tempString = tempString.replace("🍃", "")
                        }
                        if ("🌾" in it) {
                            tags.add(getString(R.string.gluten))
                            tempString = tempString.replace("🌾", "")
                        }
                        if ("🏠" in it) {
                            tags.add(getString(R.string.home_made))
                            tempString = tempString.replace("🏠", "")
                        }
                        if (tags.isNotEmpty()) {
                            added.add(tempString + " (" + tags.joinToString(", ") + ")")
                        } else {
                            added.add(tempString)
                        }
                        tempString
                    }
                } else if (element.isNotEmpty()) {
                    added.add(element)
                }
                if (added.size >= 1) {
                    content.addAll(added)
                    content.add("~~")
                }
            }
            contentPerDay.add(content.toList())
        }
        var information: String? =
            doc.select("body").select("section").select("div.white-box").select("p")[0].text()
        if (information == "") {
            information = null
        }
        menus[if (pageIndex == 0) "noon" else "evening"] = Menu(
            Day(
                days[0],
                contentPerDay[0],
                mapOf(
                    "year" to days[0].split(" ")[1].split("/")[2].toInt(),
                    "month" to days[0].split(" ")[1].split("/")[1].toInt(),
                    "day" to days[0].split(" ")[1].split("/")[0].toInt()
                )
            ),
            Day(
                days[1],
                contentPerDay[1],
                mapOf(
                    "year" to days[1].split(" ")[1].split("/")[2].toInt(),
                    "month" to days[1].split(" ")[1].split("/")[1].toInt(),
                    "day" to days[1].split(" ")[1].split("/")[0].toInt()
                )
            ),
            Day(
                days[2],
                contentPerDay[2],
                mapOf(
                    "year" to days[2].split(" ")[1].split("/")[2].toInt(),
                    "month" to days[2].split(" ")[1].split("/")[1].toInt(),
                    "day" to days[2].split(" ")[1].split("/")[0].toInt()
                )
            ),
            Day(
                days[3],
                contentPerDay[3],
                mapOf(
                    "year" to days[3].split(" ")[1].split("/")[2].toInt(),
                    "month" to days[3].split(" ")[1].split("/")[1].toInt(),
                    "day" to days[3].split(" ")[1].split("/")[0].toInt()
                )
            ),
            information,
            if (contentPerDay.size == 5)
            Day(
                days[4],
                contentPerDay[4],
                mapOf(
                    "year" to days[4].split(" ")[1].split("/")[2].toInt(),
                    "month" to days[4].split(" ")[1].split("/")[1].toInt(),
                    "day" to days[4].split(" ")[1].split("/")[0].toInt()
                )
            ) else null
        )
        showMenu(currentDay)
        if (!onReload) {
            checkForUpdates()
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