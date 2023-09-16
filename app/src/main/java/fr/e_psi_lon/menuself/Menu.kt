package fr.e_psi_lon.menuself

import org.json.JSONObject

data class Menu(
    val day1: Day = Day(),
    val day2: Day = Day(),
    val day3: Day = Day(),
    val day4: Day = Day(),
    val lastUpdate: String = "",
    val nextUpdate: String = "",
    val redactionMessage: String? = null

) {
    fun getDay(day: String): Day {
        return when (day) {
            day1.name.split(" ")[0] -> day1
            day2.name.split(" ")[0] -> day2
            day3.name.split(" ")[0] -> day3
            day4.name.split(" ")[0] -> day4
            else -> Day()
        }
    }

    override fun toString(): String {
        return "Menu: $day1, $day2, $day3, $day4 (last update: $lastUpdate, next update: $nextUpdate ${if (redactionMessage != null) ", redaction message: $redactionMessage" else ""})"
    }

    fun toJson(): String {
        return "{\"day1\": ${day1.toJson()}, \"day2\": ${day2.toJson()}, \"day3\": ${day3.toJson()}, \"day4\": ${day4.toJson()}, \"lastUpdate\": \"$lastUpdate\", \"nextUpdate\": \"$nextUpdate\"${if (redactionMessage != null) ", \"redactionMessage\": \"$redactionMessage\"" else ""}}"
    }


    companion object {
        fun fromJson(json: String): Menu {
            val jsonObject = JSONObject(json)
            return Menu(
                Day.fromJson(jsonObject.getJSONObject("day1").toString()),
                Day.fromJson(jsonObject.getJSONObject("day2").toString()),
                Day.fromJson(jsonObject.getJSONObject("day3").toString()),
                Day.fromJson(jsonObject.getJSONObject("day4").toString()),
                jsonObject.getString("lastUpdate"),
                jsonObject.getString("nextUpdate"),
                jsonObject.getString("redactionMessage") ?: null
            )
        }
    }
}
