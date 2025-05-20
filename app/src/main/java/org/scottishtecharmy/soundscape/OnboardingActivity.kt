package org.scottishtecharmy.soundscape

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import org.scottishtecharmy.soundscape.screens.onboarding.SetUpOnboardingNavGraph
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import androidx.core.content.edit

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SoundscapeTheme {
                navController = rememberNavController()
                SetUpOnboardingNavGraph(
                    navController = navController,
                    onFinish = {
                        onboardingComplete()
                    }
                )
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    fun onboardingComplete() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs.edit(commit = true) { putBoolean(MainActivity.FIRST_LAUNCH_KEY, false) }

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}