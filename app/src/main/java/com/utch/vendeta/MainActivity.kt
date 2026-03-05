package com.utch.vendeta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.utch.vendeta.ui.theme.VendetaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VendetaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VendetaTheme {
                val isLoggedIn by viewModel.isLoggedIn

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDeep
                ) {
                    AnimatedContent(
                        targetState = isLoggedIn,
                        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                        label = "navTransition"
                    ) { loggedIn ->
                        if (!loggedIn) {
                            LoginView(viewModel)
                        } else {
                            TerminalView(
                                viewModel = viewModel,
                                onLogout = { viewModel.setLogin(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
