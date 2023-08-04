package fr.e_psi_lon.menuself

data class Day(
    val name: String = "",
    val meals: List<String> = listOf()
) {
    override fun toString() : String {
        return "$name: $meals"
    }
}
