import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.R
import com.yugentech.quill.localization.UiText

/** A category's name in the app's language, or null for one Quill has no name for. [category] is its English name, the key. */
fun genreTitleRes(category: String): Int? = when (category) {
        "New Arrivals" -> R.string.new_arrivals
        "Romance" -> R.string.genre_romance
        "Philosophy" -> R.string.genre_philosophy
        "Mystery" -> R.string.genre_mystery
        "Fantasy" -> R.string.genre_fantasy
        "Adventure" -> R.string.genre_adventure
        "Science Fiction" -> R.string.genre_science_fiction
        "Shorts" -> R.string.genre_shorts
        "Horror" -> R.string.genre_horror
        "Comedy" -> R.string.genre_comedy
        "Drama" -> R.string.genre_drama
        "Biography" -> R.string.genre_biography
        "Poetry" -> R.string.genre_poetry
        "Satire" -> R.string.genre_satire
        "Memoir" -> R.string.genre_memoir
        "Autobiography" -> R.string.genre_autobiography
        "Children's" -> R.string.genre_childrens
        "Fiction" -> R.string.genre_fiction
        "Non-Fiction" -> R.string.genre_non_fiction
        "Travel" -> R.string.genre_travel
        "Spirituality" -> R.string.genre_spirituality
        else -> null
    }

/** A category's name in the app's language; an unknown one as it is. */
@Composable
fun getTitleForCategory(category: String): String = genreTitleRes(category)?.let { stringResource(it) } ?: category

/** [getTitleForCategory] for a view model. */
fun categoryText(category: String): UiText = genreTitleRes(category)?.let { UiText.Res(it) } ?: UiText.Raw(category)

/** The line under a Discover category. */
@Composable
fun getSubtitleForCategory(category: String): String {
    val id = when (category) {
        "Philosophy" -> R.string.genre_philosophy_subtitle
        "Mystery" -> R.string.genre_mystery_subtitle
        "Fantasy" -> R.string.genre_fantasy_subtitle
        "Adventure" -> R.string.genre_adventure_subtitle
        "Science Fiction" -> R.string.genre_science_fiction_subtitle
        "Shorts" -> R.string.genre_shorts_subtitle
        "Horror" -> R.string.genre_horror_subtitle
        "Comedy" -> R.string.genre_comedy_subtitle
        "Drama" -> R.string.genre_drama_subtitle
        "Biography" -> R.string.genre_biography_subtitle
        "Poetry" -> R.string.genre_poetry_subtitle
        "Satire" -> R.string.genre_satire_subtitle
        "Memoir" -> R.string.genre_memoir_subtitle
        "Autobiography" -> R.string.genre_autobiography_subtitle
        "Children's" -> R.string.genre_childrens_subtitle
        "Fiction" -> R.string.genre_fiction_subtitle
        "Non-Fiction" -> R.string.genre_non_fiction_subtitle
        "Travel" -> R.string.genre_travel_subtitle
        "Spirituality" -> R.string.genre_spirituality_subtitle
        else -> R.string.genre_fallback_subtitle // The fallback for random ones
    }
    return stringResource(id)
}
