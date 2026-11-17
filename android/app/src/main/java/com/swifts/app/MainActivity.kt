package com.swifts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.swifts.app.data.SessionStore
import com.swifts.app.data.SupabaseAuthRepository
import com.swifts.app.data.SupabaseClient
import com.swifts.app.data.SupabasePostRepository
import com.swifts.app.ui.AuthScreen
import com.swifts.app.ui.AuthViewModel
import com.swifts.app.ui.FeedScreen
import com.swifts.app.ui.FeedViewModel
import com.swifts.app.ui.theme.SwiftsTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Coil needs the SVG decoder to draw assets/icons/*.svg
        Coil.setImageLoader(
            ImageLoader.Builder(this).components { add(SvgDecoder.Factory()) }.build()
        )

        val store = SessionStore(applicationContext)
        val api = SupabaseClient(store)
        val auth = SupabaseAuthRepository(api, store)
        val posts = SupabasePostRepository(api)
        val lang = Locale.getDefault().language

        setContent {
            SwiftsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var signedIn by remember { mutableStateOf(auth.currentUserId() != null) }
                    if (!signedIn) {
                        val vm: AuthViewModel = viewModel(
                            factory = viewModelFactory { initializer { AuthViewModel(auth) } },
                        )
                        AuthScreen(vm) { signedIn = true }
                    } else {
                        val vm: FeedViewModel = viewModel(
                            factory = viewModelFactory { initializer { FeedViewModel(posts, auth, lang) } },
                        )
                        FeedScreen(vm) {
                            auth.signOut()
                            signedIn = false
                        }
                    }
                }
            }
        }
    }
}
