package com.swifts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.swifts.app.data.SupabaseFeedRepository
import com.swifts.app.ui.FeedScreen
import com.swifts.app.ui.FeedViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = SupabaseFeedRepository(accessToken = { null })
        setContent {
            MaterialTheme {
                Surface { FeedScreen(FeedViewModel(repo, lang = Locale.getDefault().language)) }
            }
        }
    }
}
