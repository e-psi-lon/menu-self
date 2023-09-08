package fr.e_psi_lon.menuself

import org.json.JSONObject

data class Day(
    val name: String = "",
    val meals: List<String> = listOf()
) {
    override fun toString(): String {
        return "$name: $meals"
    }

    fun toJson(): String {
        return "{\"name\": \"$name\", \"meals\": ${
            meals.joinToString(
                prefix = "[",
                postfix = "]"
            ) { "\"$it\"" }
        }}"
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
                meals
            )
        }
    }
}
