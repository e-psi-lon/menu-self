package fr.e_psi_lon.menuself.others

fun String.capitalize(): String {
    return when (this.length) {
        0 -> this
        1 -> this.uppercase()
        else -> this[0].uppercaseChar() + this.substring(1).lowercase()
    }
}