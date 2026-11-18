package com.swifts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.swifts.app.data.SessionStore
import com.swifts.app.data.SupabaseAuthRepository
import com.swifts.app.data.SupabaseClient
import com.swifts.app.data.SupabaseCommentRepository
import com.swifts.app.data.SupabaseGroupRepository
import com.swifts.app.data.SupabaseMediaStorage
import com.swifts.app.data.SupabaseMessageRepository
import com.swifts.app.data.SupabaseNotificationRepository
import com.swifts.app.data.SupabasePostRepository
import com.swifts.app.data.SupabaseProfileRepository
import com.swifts.app.data.SupabaseStoryRepository
import com.swifts.app.ui.AppServices
import com.swifts.app.ui.AuthScreen
import com.swifts.app.ui.AuthViewModel
import com.swifts.app.ui.SwiftsShell
import com.swifts.app.ui.theme.SwiftsTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Coil.setImageLoader(
            ImageLoader.Builder(this).components { add(SvgDecoder.Factory()) }.build()
        )

        val store = SessionStore(applicationContext)
        val api = SupabaseClient(store)
        val auth = SupabaseAuthRepository(api, store)
        val services = AppServices(
            auth = auth,
            posts = SupabasePostRepository(api),
            profiles = SupabaseProfileRepository(api),
            groups = SupabaseGroupRepository(api),
            stories = SupabaseStoryRepository(api),
            messages = SupabaseMessageRepository(api),
            notifications = SupabaseNotificationRepository(api),
            comments = SupabaseCommentRepository(api),
            storage = SupabaseMediaStorage(api),
            lang = Locale.getDefault().language,
        )

        setContent {
            SwiftsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Driven by the stored session. Signing in or out, or an expired session, switches screens here.
                    val signedIn by store.signedIn.collectAsState()
                    if (!signedIn) {
                        val vm: AuthViewModel = viewModel(
                            factory = viewModelFactory { initializer { AuthViewModel(auth) } },
                        )
                        AuthScreen(vm) { }
                    } else {
                        SwiftsShell(services = services, onSignOut = { auth.signOut() })
                    }
                }
            }
        }
    }
}
