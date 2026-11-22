package com.yugentech.quill

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import com.yugentech.quill.ui.common.components.LocalSourcesDown
import org.koin.compose.koinInject
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.localization.AppLocale
import com.yugentech.quill.navigation.host.AppNavHost
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.theme.QuillTheme
import com.yugentech.theme.ThemeRevealHost
import com.yugentech.theme.models.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModel()
    private var shouldNavigateToHome by mutableStateOf(false)

    // Below Android 13, the language picked in the app (see AppLocale); from 13 the system's
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    // Something may have been bought, redeemed or cancelled in the Play Store while the app
    // was in the background, so Play is asked again each time it comes back to the front
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { get<BillingRepository>().refreshPurchases() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // The splash's minimum time is for launching the app only. When the activity is created
        // again (savedInstanceState is set: a configuration change Android applied by recreating
        // it, which a language change can do), there's no splash to show, and holding the first
        // frame back for a second just leaves the window blank. The view models survive the
        // recreation, so the screen is ready to draw straight away.
        val isLaunch = savedInstanceState == null
        val animationReady = MutableStateFlow(!isLaunch)
        if (isLaunch) {
            lifecycleScope.launch {
                delay(1000.milliseconds)
                animationReady.value = true
            }
        }

        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            !animationReady.value ||
                    authViewModel.authState.value.isInitializing ||
                    authViewModel.showOnboarding.value == null
        }

        setContent {
            val navController = rememberNavController()

            val showOnboarding by authViewModel.showOnboarding.collectAsStateWithLifecycle()
            val authState by authViewModel.authState.collectAsStateWithLifecycle()

            val themeViewModel: ThemeViewModel = koinViewModel()
            val themeConfiguration by themeViewModel.themeConfiguration.collectAsStateWithLifecycle()
            val hapticsEnabled by themeViewModel.hapticsEnabled.collectAsStateWithLifecycle()
            val darkTheme = when (themeConfiguration.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            enableEdgeToEdge(
                statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(scrim = Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT)
                },
                navigationBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(scrim = Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT)
                }
            )

            // Which sources are down for maintenance, for every cover in the app
            val maintenanceMonitor = koinInject<SourceMaintenanceMonitor>()
            val sourcesDown by maintenanceMonitor.down.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { maintenanceMonitor.checkAllIfDue() }

            QuillTheme(
                themeConfiguration = themeConfiguration,
                hapticsEnabled = hapticsEnabled
            ) {
              CompositionLocalProvider(LocalSourcesDown provides sourcesDown.keys) {
                // Outermost, so a light/dark wipe covers the whole screen.
                ThemeRevealHost {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val authState by authViewModel.authState.collectAsStateWithLifecycle()

                        if (showOnboarding != null && !authState.isInitializing) {
                            AppNavHost(
                                navController = navController,
                                webClientId = BuildConfig.WEB_CLIENT_ID,
                                authViewModel = authViewModel,
                                themeViewModel = themeViewModel,
                                showOnboarding = showOnboarding!!,
                                onOnboardingComplete = {
                                    authViewModel.completeOnboarding()
                                },
                                shouldNavigateToHome = shouldNavigateToHome,
                                onNavigatedToHome = {
                                    shouldNavigateToHome = false
                                }
                            )
                        }
                    }
                }
              }
            }
        }
    }
}