package fr.e_psi_lon.menuself.activity


import android.view.View
import android.widget.ArrayAdapter
import fr.e_psi_lon.menuself.R
import fr.e_psi_lon.menuself.data.Day
import fr.e_psi_lon.menuself.data.Menu
import fr.e_psi_lon.menuself.data.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.util.Calendar


class EveningActivity : MenuActivity(21, 1) {
    override fun fetchMenuData(onReload: Boolean) =
        CoroutineScope(Dispatchers.IO).launch {
            if (menus["evening"] != Menu()) {
                showMenu(currentDay)
                return@launch
            }
            val doc: Document =
                Jsoup.connect("https://filtreimages.neocities.org/menu-soir")
                    .get()
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
                    } else {
                        added.add(element)
                    }
                    if (added.size >= 1) {
                        content.addAll(added)
                        content.add("~~")
                    }
                }
                contentPerDay.add(content.toList())
            }
            var redactionMessage: String? =
                doc.select("body").select("section").select("div.white-box").select("p")[0].text()
            if (redactionMessage == "") {
                redactionMessage = null
            }

            menus["evening"] = Menu(
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
                redactionMessage = redactionMessage
            )
            showMenu(currentDay)
            if (!onReload) {
                checkForUpdates()
            }
        }

    override fun showMenu(day: String) = CoroutineScope(Dispatchers.Main).launch {
        if (day == "Saturday" || day == "Sunday" || day == "Friday") {
            dayView.text = getTranslatedString(day)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        if (!Request.isNetworkAvailable(this@EveningActivity.applicationContext)) {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_internet)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        if (menus["evening"]?.getDay(getFrench(day))?.name == "") {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_day_data)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }

        if (menus["evening"]?.getDay(getFrench(day))?.meals?.isEmpty() == true) {
            dayView.text =
                menus["evening"]?.getDay(getFrench(day))?.name ?: getTranslatedString(day)
            menuListView.visibility = View.GONE
            statusView.text =
                getString(R.string.no_menu_this_day, menus["evening"]?.getDay(getFrench(day))?.name)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        try {
            dayView.text =
                menus["evening"]?.getDay(getFrench(day))?.name ?: getTranslatedString(day)
            menuListView.adapter = menus["evening"]?.getDay(getFrench(day))?.let {
                ArrayAdapter(
                    this@EveningActivity,
                    android.R.layout.simple_list_item_1, it.meals
                )
            }
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
            withContext(Dispatchers.IO) {
                file.createNewFile()
            }
        }
        val json =
            "{\"date\":\"$date\"${if (eveningMenu != null) ",\"evening_menu\":$eveningMenu" else ""}${if (noonMenu != null) ",\"noon_menu\":$noonMenu" else ""}}"
        withContext(Dispatchers.IO) {
            file.writeText(json)
        }
    }
}