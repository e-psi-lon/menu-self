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
        return "Menu: $day1,\n$day2,\n$day3,\n$day4${
            if (day5 != null) {
                ",\n$day5"
            } else {
                ""
            }
        } (last update: $lastUpdate,\nnext update: $nextUpdate ${if (redactionMessage != null) ",\nredaction message: $redactionMessage" else ""})"
    }

    fun toJson(): String {
        return "{\"day1\": ${day1.toJson()}, \"day2\": ${day2.toJson()}, \"day3\": ${day3.toJson()}, \"day4\": ${day4.toJson()},${
            if (day5 != null) {
                " \"day5\": ${day5.toJson()},"
            } else ""
        } \"lastUpdate\": \"$lastUpdate\", \"nextUpdate\": \"$nextUpdate\"${if (redactionMessage != null) ", \"redactionMessage\": \"$redactionMessage\"" else ""}}"
    }

    override fun equals(other: Any?): Boolean {
        if (other is Menu) {
            return day1 == other.day1 && day2 == other.day2 && day3 == other.day3 && day4 == other.day4 && day5 == other.day5 && lastUpdate == other.lastUpdate && nextUpdate == other.nextUpdate && redactionMessage == other.redactionMessage
        }
        return false
    }

    override fun hashCode(): Int {
        var result = day1.hashCode()
        result = 31 * result + day2.hashCode()
        result = 31 * result + day3.hashCode()
        result = 31 * result + day4.hashCode()
        result = 31 * result + lastUpdate.hashCode()
        result = 31 * result + nextUpdate.hashCode()
        result = 31 * result + (redactionMessage?.hashCode() ?: 0)
        result = 31 * result + (day5?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun fromJson(json: String): Menu {
            val jsonObject = JSONObject(json)
            return if (jsonObject.has("redactionMessage")) {
                if (jsonObject.has("day5")) {
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
            } else {
                if (jsonObject.has("day5")) {
                    Menu(
                        Day.fromJson(jsonObject.getString("day1")),
                        Day.fromJson(jsonObject.getString("day2")),
                        Day.fromJson(jsonObject.getString("day3")),
                        Day.fromJson(jsonObject.getString("day4")),
                        jsonObject.getString("lastUpdate"),
                        jsonObject.getString("nextUpdate"),
                        day5 = Day.fromJson(jsonObject.getString("day5"))
                    )
                } else {
                    Menu(
                        Day.fromJson(jsonObject.getString("day1")),
                        Day.fromJson(jsonObject.getString("day2")),
                        Day.fromJson(jsonObject.getString("day3")),
                        Day.fromJson(jsonObject.getString("day4")),
                        jsonObject.getString("lastUpdate"),
                        jsonObject.getString("nextUpdate")
                    )
                }
            }
        }
    }
}
