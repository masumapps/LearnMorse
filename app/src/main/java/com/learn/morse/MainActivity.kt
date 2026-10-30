  package com.learn.morse

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.learn.morse.audio.MorsePlayer
import com.learn.morse.language.AppLanguage
import com.learn.morse.monetization.Ads
import com.learn.morse.monetization.AppOpenAds
import com.learn.morse.monetization.Billing
import com.learn.morse.monetization.Entitlements
import com.learn.morse.navigation.AppNavHost
import com.learn.morse.reminder.DailyReminder
import com.learn.morse.ui.theme.AppTheme
import com.learn.morse.ui.theme.LearnMorseTheme
import com.learn.morse.ui.theme.isDarkTheme
import com.learn.morse.ui.theme.morseBackground

class MainActivity : ComponentActivity() {
    // Before Android 13, the language chosen in the app is applied here.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug builds only; must come before anything reads progress or Premium.
        DemoMode.applyIfRequested(this, intent)
        Entitlements.init(this)
        Billing.init(this)
        DailyReminder.ensureScheduled(this)
        AppTheme.init(this)
        enableEdgeToEdge()
        setContent {
            val dark = isDarkTheme()
            // Status and navigation bar icons follow the app's theme, which can differ from the phone's.
            LaunchedEffect(dark) {
                val style = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            LearnMorseTheme(darkTheme = dark) {
                Box {
                    Scaffold(modifier = Modifier.fillMaxSize().morseBackground(), containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) { innerPadding ->
                        // Consume the insets so screens with their own Scaffold/TopAppBar don't pad for the status bar again.
                        AppNavHost(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding))
                    }
                    ScreenFlash()
                }
            }
        }
        // Only once the window is set up: starting the ad SDK loads WebView, which updates the app's
        // resources on a background thread. Done before, that raced the window's own setup and now
        // and then left it without a content view, crashing on launch.
        Ads.init(this)
        AppOpenAds.init(this)
    }

    override fun onResume() {
        super.onResume()
        // Picks up purchases that finished while the app was away, e.g. a pending payment.
        Billing.refreshPurchases()
    }
}

/** Lights up the whole screen while morse plays with the screen flash output on. Lets touches through. */
@Composable
private fun ScreenFlash() {
    val flashOn by MorsePlayer.flashOn.collectAsState()
    if (flashOn) {
        Box(
            Modifier
                .fillMaxSize()
                // Translucent, so the screen stays readable, and gentler on the eyes than full white.
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LearnMorseTheme {
        Greeting("Android")
    }
}