package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class SelectionBridge(private val onSelectionChanged: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun onTextChange(text: String) {
        onSelectionChanged(text)
    }
}

@OptIn(ExperimentalReadiumApi::class)
fun attachNavigator(
    activity: FragmentActivity,
    wrapperView: ReadiumWrapperView,
    containerId: Int,
    fragmentTag: String,
    publication: Publication,
    initialLocation: Locator?,
    preferences: EpubPreferences,
    onTap: () -> Unit,
    onNavigatorReady: (EpubNavigatorFragment) -> Unit
) {
    val fm = activity.supportFragmentManager

    var fragment = fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment

    if (fragment == null) {
        val factory = EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocation,
            initialPreferences = preferences,
            configuration = buildNavigatorConfig(wrapperView)
        )

        fm.fragmentFactory = factory
        fragment = fm.fragmentFactory.instantiate(
            activity.classLoader,
            EpubNavigatorFragment::class.java.name
        ) as EpubNavigatorFragment

        fm.commitNow { replace(containerId, fragment, fragmentTag) }
    }

    fragment.addInputListener(object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            onTap()
            return true
        }
    })

    onNavigatorReady(fragment)
}

@OptIn(ExperimentalReadiumApi::class)
fun buildNavigatorConfig(wrapperView: ReadiumWrapperView) = EpubNavigatorFragment.Configuration().apply {
    servedAssets = servedAssets + "font/.*"
    shouldApplyInsetsPadding = false
    registerFonts(this)
    registerHighlightStyles(this)
    registerJavascriptInterface("quillSelection") { SelectionBridge { wrapperView.onSelectionChanged(it) } }
}

@OptIn(ExperimentalReadiumApi::class)
fun registerHighlightStyles(config: EpubNavigatorFragment.Configuration) {
    config.decorationTemplates.set(UnderlineStyle::class, HtmlDecorationTemplate(
        layout = HtmlDecorationTemplate.Layout.BOXES,
        element = { decoration ->
            val style = decoration.style as UnderlineStyle
            val color = Color(style.tint)
            val cssColor = "rgba(${(color.red * 255).toInt()}, ${(color.green * 255).toInt()}, ${(color.blue * 255).toInt()}, 0.9)"
            """<div style="width:100%;height:100%;box-sizing:border-box;border-bottom:2px solid $cssColor;"></div>"""
        }
    ))

}

@OptIn(ExperimentalReadiumApi::class)
fun registerFonts(config: EpubNavigatorFragment.Configuration) {
    fun addFont(family: FontFamily, file: String) {
        config.addFontFamilyDeclaration(family) {
            addFontFace { addSource("font/$file", preload = true); setFontStyle(FontStyle.NORMAL) }
        }
    }

    addFont(ReaderDefaults.FONT_GOOGLE_SANS, "google_sans_flex.ttf")
    addFont(ReaderDefaults.FONT_LITERATA, "literata.ttf")
    addFont(ReaderDefaults.FONT_GOUDY, "goudy.ttf")
    addFont(ReaderDefaults.FONT_GARAMOND, "eb_garamond.ttf")
}