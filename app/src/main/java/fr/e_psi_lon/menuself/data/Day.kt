package fr.e_psi_lon.menuself.data

import org.json.JSONObject

data class Day(
    val name: String = "",
    val meals: List<String> = listOf(),
    val date: Map<String, Int> = mapOf(
        "year" to 0,
        "month" to 0,
        "day" to 0
    )
) {

    override fun toString(): String {
        return "$name: $meals (${date["day"]}/${date["month"]}/${date["year"]})"
    }

    fun toJson(): String {
        return "{\"name\": \"$name\", \"meals\": ${
            meals.joinToString(
                prefix = "[",
                postfix = "]"
            ) { "\"$it\"" }
        }, \"date\": {\"year\": ${date["year"]}, \"month\": ${date["month"]}, \"day\": ${date["day"]}}}"
    }

    override fun equals(other: Any?): Boolean {
        if (other is Day) {
            return name == other.name && meals == other.meals && date == other.date
        }
        return false
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + meals.hashCode()
        result = 31 * result + date.hashCode()
        return result
    }


    companion object {
        fun fromJson(json: String): Day {
            val jsonObject = JSONObject(json)
            val meals = mutableListOf<String>()
            for (i in 0 until jsonObject.getJSONArray("meals").length()) {
                meals += jsonObject.getJSONArray("meals").getString(i)
            }
            return Day(
                jsonObject.getString("name"),
                meals,
                mapOf(
                    "year" to jsonObject.getJSONObject("date").getInt("year"),
                    "month" to jsonObject.getJSONObject("date").getInt("month"),
                    "day" to jsonObject.getJSONObject("date").getInt("day")
                )
            )
        }
    }
}
