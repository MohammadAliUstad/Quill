package com.yugentech.quill.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yugentech.quill.reader.ui.parent.ReaderScreen
import com.yugentech.quill.reader.viewmodel.ReaderViewModel
import com.yugentech.theme.QuillTheme
import com.yugentech.theme.ThemeRepository
import com.yugentech.theme.models.ThemeConfiguration
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReaderActivity : AppCompatActivity() {

    private val viewModel: ReaderViewModel by viewModel()
    private val themeRepository: ThemeRepository by inject()
    private lateinit var insetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID) ?: run { finish(); return }
        val initialChapterHref = intent.getStringExtra(EXTRA_INITIAL_HREF)

        setupWindow()
        viewModel.loadBook(bookId, initialChapterHref)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val preferences by viewModel.readerPreferences.collectAsState() // 1. Collect preferences
            val currentThemeConfig by themeRepository.themeConfiguration.collectAsState(
                initial = ThemeConfiguration()
            )

            QuillTheme(themeConfiguration = currentThemeConfig) {
                ReaderScreen(
                    uiState = uiState,
                    onBackClick = { finish() },
                    preferences = preferences, // 2. Pass them down
                    onPreferencesChange = { viewModel.updatePreferences(it) },
                    onLocatorChange = { locator -> viewModel.saveProgress(bookId, locator) },
                    onMenuVisibilityChange = { visible -> setSystemBarsVisible(visible) }
                )
            }
        }
    }

    private fun setupWindow() {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        insetsController = WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun setSystemBarsVisible(visible: Boolean) {
        if (visible) insetsController.show(WindowInsetsCompat.Type.systemBars())
        else insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        private const val EXTRA_BOOK_ID = "extra_book_id"
        private const val EXTRA_INITIAL_HREF = "extra_initial_href"

        fun createIntent(context: Context, bookId: String, initialChapterHref: String? = null): Intent =
            Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_BOOK_ID, bookId)
                putExtra(EXTRA_INITIAL_HREF, initialChapterHref)
            }
    }
}