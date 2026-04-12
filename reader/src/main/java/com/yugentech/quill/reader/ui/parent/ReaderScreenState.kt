package com.yugentech.quill.reader.ui.parent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yugentech.quill.reader.ui.components.overlay.parent.ReaderOverlayState
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@Stable
class ReaderScreenState(
    private val publication: Publication,
    private val allPositions: List<Locator>,
    private val totalPages: Int,
    initialLocator: Locator?
) {
    // --- UI Visibility States ---
    var isMenuVisible by mutableStateOf(false)
    var showSettingsSheet by mutableStateOf(false)
    var showTocSheet by mutableStateOf(false)
    var isBrightnessInteracting by mutableStateOf(false)
    var showAiraPeek by mutableStateOf(false)
    
    // --- Navigation & Playback States ---
    var selectedText by mutableStateOf<String?>(null)
    var targetJumpHref by mutableStateOf<String?>(null)
    var pendingSeekProgress by mutableStateOf<Double?>(null)
    var currentLocator by mutableStateOf<Locator?>(initialLocator)
    var isScrubbing by mutableStateOf(false)

    // --- Chapter Memory (Business Logic for Auto-Jumping) ---
    private val chapterProgressMap = mutableMapOf<String, Locator>()
    private var previousHref by mutableStateOf<String?>(null)
    var isHandlingAutoJump by mutableStateOf(false)
    var isExplicitJump by mutableStateOf(false)
    var targetLocator by mutableStateOf<Locator?>(null)

    // --- Derived States ---
    private val currentChapterIndex: Int
        get() {
            val currentHref = currentLocator?.href?.toString()?.substringBefore("#") ?: return 0
            return publication.readingOrder
                .indexOfFirst { it.href.toString().substringBefore("#") == currentHref }
                .coerceAtLeast(0)
        }

    private val displayTitle: String
        get() = currentLocator?.title ?: publication.metadata.title ?: "Chapter"

    private val chapterPagesLeft: Int
        get() {
            val href = currentLocator?.href ?: return 0
            val chapterPositions = allPositions.filter { it.href == href }
            val currentProgression = currentLocator?.locations?.progression ?: 0.0
            val currentIndex = chapterPositions
                .indexOfLast { (it.locations.progression ?: 0.0) <= currentProgression }
                .coerceAtLeast(0)
            return (chapterPositions.size - 1 - currentIndex).coerceAtLeast(0)
        }

    val overlayState: ReaderOverlayState
        get() = ReaderOverlayState(
            bookTitle = publication.metadata.title ?: "Book",
            chapterTitle = displayTitle,
            chapterPagesLeft = chapterPagesLeft,
            progress = (currentLocator?.locations?.totalProgression ?: 0.0).toFloat(),
            totalPages = totalPages,
            currentChapterIndex = currentChapterIndex,
            selectedText = selectedText
        )

    // --- Intent Functions (Actions) ---
    fun toggleMenu() {
        if (showAiraPeek) {
            showAiraPeek = false
            selectedText = null
        } else {
            isMenuVisible = !isMenuVisible
        }
    }

    fun showAira(text: String?) {
        selectedText = text
        showAiraPeek = true
        isMenuVisible = false 
    }

    fun dismissAira() {
        showAiraPeek = false
        selectedText = null
    }

    fun handleLocatorChange(newLocator: Locator) {
        currentLocator = newLocator
        val currentHref = newLocator.href.toString()

        if (isHandlingAutoJump) {
            isHandlingAutoJump = false
            previousHref = currentHref
            chapterProgressMap[currentHref] = newLocator
            return
        }

        if (isExplicitJump) {
            isExplicitJump = false
            previousHref = currentHref
            chapterProgressMap[currentHref] = newLocator
            return
        }

        if (previousHref != null && previousHref != currentHref) {
            val savedLocator = chapterProgressMap[currentHref]
            if (savedLocator != null) {
                val currentProg = newLocator.locations.progression ?: 0.0
                val savedProg = savedLocator.locations.progression ?: 0.0

                if (Math.abs(currentProg - savedProg) > 0.01) {
                    isHandlingAutoJump = true
                    targetLocator = savedLocator
                    return
                }
            }
        }

        previousHref = currentHref
        chapterProgressMap[currentHref] = newLocator
    }
}

@Composable
fun rememberReaderScreenState(
    publication: Publication,
    allPositions: List<Locator>,
    totalPages: Int,
    initialLocator: Locator?
): ReaderScreenState {
    return remember(publication, allPositions, totalPages) {
        ReaderScreenState(publication, allPositions, totalPages, initialLocator)
    }
}