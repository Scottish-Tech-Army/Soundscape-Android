package org.scottishtecharmy.soundscape

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.datastore.DataStoreManager
import org.scottishtecharmy.soundscape.datastore.DataStoreManager.PreferencesKeys.FIRST_LAUNCH
import org.scottishtecharmy.soundscape.screens.onboarding.SetUpOnboardingNavGraph

import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var navController: NavHostController
    @Inject lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SoundscapeTheme {
                navController = rememberNavController()
                SetUpOnboardingNavGraph(
                    navController = navController
                )
            }
        }
    }

    fun onboardingComplete() {
        runBlocking {
            dataStoreManager.setValue(
                FIRST_LAUNCH,
                false
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}