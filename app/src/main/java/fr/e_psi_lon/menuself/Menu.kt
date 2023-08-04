package fr.e_psi_lon.menuself

data class Menu(
    val day1: Day = Day(),
    val day2: Day = Day(),
    val day3: Day = Day(),
    val day4: Day = Day()
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

    override fun toString() : String {
        return "Menu: $day1, $day2, $day3, $day4"

    }
}
