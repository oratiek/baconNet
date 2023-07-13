package io.baconnet

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.baconnet.ui.pages.FirstTime
import io.baconnet.ui.pages.Post
import io.baconnet.ui.pages.Timeline
import io.baconnet.ui.theme.Bacon_netTheme

class MainActivity : ComponentActivity() {
    lateinit var navController: NavController

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.initDataIfNotExists()

        setContent {
            Bacon_netTheme {
                val activity = LocalContext.current as MainActivity
                val navController = rememberNavController()
                this.navController = navController

                val startDestination =
                    if (activity.getDisplayName() == "") "first_time" else "timeline"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("first_time") { FirstTime() }
                    composable("timeline") {
                        Timeline()
                    }
                    composable("post") { Post() }
                }
            }
        }
    }

    private fun initDataIfNotExists() {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        val displayName = pref.getString(getString(R.string.key_display_name), "") ?: ""

        with(pref.edit()) {

            if (displayName == "") {
                putString(getString(R.string.key_display_name), "")
            }

            apply()
        }
    }

    public fun getDisplayName(): String {
        val pref = this.getPreferences(Context.MODE_PRIVATE)

        return pref.getString(getString(R.string.key_display_name), "") ?: ""
    }

    public fun setDisplayName(displayName: String) {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putString(getString(R.string.key_display_name), displayName)
            apply()
        }
    }

    public fun navigateToTimeline() {
        navController.navigate("timeline")
    }

    public fun navigateToPost() {
        navController.navigate("post")
    }
}