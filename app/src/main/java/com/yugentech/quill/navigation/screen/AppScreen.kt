package com.yugentech.quill.navigation.screen

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.yugentech.quill.database.converter.AppJson
import com.yugentech.quill.database.model.Book
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class AppScreen(val route: String) {
    data object Onboarding : AppScreen("onboarding")
    data object SignIn : AppScreen("sign_in")
    data object SignUp : AppScreen("sign_up")
    data object Main : AppScreen("main")
    data object AllBooks : AppScreen("all_books")
    data object StandardEbooks : AppScreen("sources/standard_ebooks")
    data object Gutenberg : AppScreen("sources/gutenberg")
    data object Aira : AppScreen("aira")
    data object Queue : AppScreen("queue")
    data object Appearance : AppScreen("appearance")
    data object ManageCategories : AppScreen("manageCategories")
    data object Storage : AppScreen("storage")
    data object About : AppScreen("about")
    data object AboutAira : AppScreen("about_aira")
    data object MoreApps : AppScreen("more_apps")
    data object Licenses : AppScreen("licenses")
    data object Insights : AppScreen("insights")
    data object EditProfileScreen : AppScreen("edit_profile")
    data object Subscriptions : AppScreen("subscriptions")
    data object NotesScreen : AppScreen("notes")

    data object BookDetailsScreen : AppScreen("bookDetailsScreen") {

        const val ROUTE = "bookDetailsScreen?bookId={bookId}&bookJson={bookJson}"

        val arguments: List<NamedNavArgument> = listOf(
            navArgument("bookId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("bookJson") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )

        fun createRoute(bookId: String): String {
            val encodedId = URLEncoder.encode(bookId, StandardCharsets.UTF_8.toString())
            return "bookDetailsScreen?bookId=$encodedId"
        }

        fun createRoute(book: Book): String {
            val json = AppJson.encodeToString(book)
            val encodedJson = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
            return "bookDetailsScreen?bookJson=$encodedJson"
        }
    }
}