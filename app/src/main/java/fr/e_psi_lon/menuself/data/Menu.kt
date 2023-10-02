package fr.e_psi_lon.menuself.data

import org.json.JSONObject

data class Menu(
    val day1: Day = Day(),
    val day2: Day = Day(),
    val day3: Day = Day(),
    val day4: Day = Day(),
    val lastUpdate: String = "",
    val nextUpdate: String = "",
    val redactionMessage: String? = null,
    val day5: Day? = null

) {
    fun getDay(day: String): Day {
        return when (day) {
            day1.name.split(" ")[0] -> day1
            day2.name.split(" ")[0] -> day2
            day3.name.split(" ")[0] -> day3
            day4.name.split(" ")[0] -> day4
            day5?.name?.split(" ")?.get(0) -> day5
            else -> Day()
        }
    }

    override fun toString(): String {
        return "Menu: $day1, $day2, $day3, $day4${
            if (day5 != null) {
                ", $day5"
            } else {
                ""
            }
        } (last update: $lastUpdate, next update: $nextUpdate ${if (redactionMessage != null) ", redaction message: $redactionMessage" else ""})"
    }

    fun toJson(): String {
        return "{\"day1\": ${day1.toJson()}, \"day2\": ${day2.toJson()}, \"day3\": ${day3.toJson()}, \"day4\": ${day4.toJson()},${
            if (day5 != null) {
                " \"day5\": ${day5.toJson()},"
            } else ""
        } \"lastUpdate\": \"$lastUpdate\", \"nextUpdate\": \"$nextUpdate\"${if (redactionMessage != null) ", \"redactionMessage\": \"$redactionMessage\"" else ""}}"
    }


    companion object {
        fun fromJson(json: String): Menu {
            val jsonObject = JSONObject(json)
            return if (jsonObject.has("day5")) {
                Menu(
                    Day.fromJson(jsonObject.getString("day1")),
                    Day.fromJson(jsonObject.getString("day2")),
                    Day.fromJson(jsonObject.getString("day3")),
                    Day.fromJson(jsonObject.getString("day4")),
                    jsonObject.getString("lastUpdate"),
                    jsonObject.getString("nextUpdate"),
                    jsonObject.getString("redactionMessage"),
                    Day.fromJson(jsonObject.getString("day5"))
                )
            } else {
                Menu(
                    Day.fromJson(jsonObject.getString("day1")),
                    Day.fromJson(jsonObject.getString("day2")),
                    Day.fromJson(jsonObject.getString("day3")),
                    Day.fromJson(jsonObject.getString("day4")),
                    jsonObject.getString("lastUpdate"),
                    jsonObject.getString("nextUpdate"),
                    jsonObject.getString("redactionMessage")
                )
            }
        }
    }
}
