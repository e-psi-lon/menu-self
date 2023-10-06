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


class NoonActivity : MenuActivity(15, 0) {
    override fun fetchMenuData() = CoroutineScope(Dispatchers.IO).launch {
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

    override fun showMenu(day: String) = CoroutineScope(Dispatchers.Main).launch {
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

}