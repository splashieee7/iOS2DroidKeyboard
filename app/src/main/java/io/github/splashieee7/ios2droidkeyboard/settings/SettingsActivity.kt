package io.github.splashieee7.ios2droidkeyboard.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        isEnabled = isKeyboardEnabled(),
                        onOpenKeyboardSettings = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        },
                        onChooseKeyboard = {
                            inputMethodManager().showInputMethodPicker()
                        },
                    )
                }
            }
        }
    }

    private fun inputMethodManager(): InputMethodManager =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    /** True once the user has ticked this keyboard on in system settings. */
    private fun isKeyboardEnabled(): Boolean =
        inputMethodManager().enabledInputMethodList.any { it.packageName == packageName }
}

@Composable
private fun SettingsScreen(
    isEnabled: Boolean,
    onOpenKeyboardSettings: () -> Unit,
    onChooseKeyboard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "iOS2DroidKeyBoard", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = if (isEnabled) "Status: enabled" else "Status: not enabled yet",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(text = "1. Enable the keyboard", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onOpenKeyboardSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open keyboard settings")
        }

        Text(text = "2. Switch to it", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onChooseKeyboard, modifier = Modifier.fillMaxWidth()) {
            Text("Choose keyboard")
        }
    }
}
