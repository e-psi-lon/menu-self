package fr.e_psi_lon.menuself.others

fun String.capitalize(): String {
    return this[0].uppercaseChar() + this.substring(1).lowercase()
}