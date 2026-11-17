package com.swifts.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swifts.app.R
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun AuthScreen(vm: AuthViewModel, onSignedIn: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val colors = LocalSwiftsColors.current

    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.primaryText,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.mutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { vm.signInWithGoogle(context.findActivity()) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.continue_with_google), style = MaterialTheme.typography.labelLarge)
            }
        }

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Google sign-in needs an Activity context")
}
