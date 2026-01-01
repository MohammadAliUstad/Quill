package com.yugentech.quill.navigation

sealed class Screens(val route: String) {
    data object Appearance : Screens("appearance")
    data object Main : Screens("main")
    data object About : Screens("about")
    data object ManageCategories : Screens("manageCategories")
    data object Licenses : Screens("licenses")
    data object Aira : Screens("Aira")
    data object BookDetailsScreen : Screens("bookDetailsScreen")
    object StandardEbooks : Screens("sources/standard_ebooks")
}