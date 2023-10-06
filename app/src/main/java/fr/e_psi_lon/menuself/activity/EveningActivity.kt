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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


class EveningActivity : MenuActivity(21, 1) {
    override fun fetchMenuData() = CoroutineScope(Dispatchers.IO).launch {
        val doc: Document =
            Jsoup.connect("https://standarddelunivers.wordpress.com/2022/06/28/menu-de-la-semaine/")
                .get()
        val tables: MutableList<Element> = doc.select("table").toMutableList()
            .subList(doc.select("table").size - 2, doc.select("table").size)
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
            tableI += 2
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

        eveningMenu = Menu(
            Day(days[0], contentPerDay[0]),
            Day(days[1], contentPerDay[1]),
            Day(days[2], contentPerDay[2]),
            Day(days[3], contentPerDay[3]),
            lastMenuUpdate,
            nextMenuUpdate,
            redactionMessage
        )
        showMenu(currentDay)


    }

    override fun showMenu(day: String) = CoroutineScope(Dispatchers.Main).launch {
        if (day == "Saturday" || day == "Sunday") {
            dayView.text = getTranslatedString(day)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_menu_this_day, getTranslatedString(day))
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }
        if (day == "Friday") {
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
        if (eveningMenu.getDay(getFrench(day)).name == "") {
            dayView.text = getTranslatedString(currentDay)
            menuListView.visibility = View.GONE
            statusView.text = getString(R.string.no_day_data)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }

        if (eveningMenu.getDay(getFrench(day)).meals.isEmpty()) {
            dayView.text = eveningMenu.getDay(getFrench(day)).name
            menuListView.visibility = View.GONE
            statusView.text =
                getString(R.string.no_menu_this_day, eveningMenu.getDay(getFrench(day)).name)
            statusView.visibility = View.VISIBLE
            menuLayout.isRefreshing = false
            return@launch
        }

        try {
            dayView.text = eveningMenu.getDay(getFrench(day)).name
            menuListView.adapter = ArrayAdapter(
                this@EveningActivity,
                android.R.layout.simple_list_item_1, eveningMenu.getDay(getFrench(day)).meals
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


}