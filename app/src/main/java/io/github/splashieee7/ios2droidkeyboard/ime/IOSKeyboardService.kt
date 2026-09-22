package io.github.splashieee7.ios2droidkeyboard.ime

import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.inputmethodservice.InputMethodService
import io.github.splashieee7.ios2droidkeyboard.ui.KeyboardScreen
import kotlin.math.abs

/**
 * Hosts the keyboard UI.
 *
 * An IME window is not an Activity, so nothing populates the ViewTree owners that Compose
 * looks for when it attaches. We own all three lifecycles ourselves and put them on both the
 * decor view and the ComposeView; without them Compose throws as soon as the keyboard opens.
 */
class IOSKeyboardService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    KeyboardActions {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        // Must restore before the registry is observed, and before super wires up the window.
        savedStateController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IOSKeyboardService)
            setViewTreeViewModelStoreOwner(this@IOSKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@IOSKeyboardService)
            setContent {
                KeyboardScreen(actions = this@IOSKeyboardService)
            }
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    // --- KeyboardActions ---
    // Nothing typed is logged here or anywhere downstream, in any build type.

    override fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun deleteBackward(count: Int) {
        if (count <= 0) return
        currentInputConnection?.deleteSurroundingText(count, 0)
    }

    override fun textBeforeCursor(maxChars: Int): String =
        currentInputConnection?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()

    override fun moveCursor(steps: Int) {
        val connection = currentInputConnection ?: return
        val keyCode = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(abs(steps)) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    override fun performReturn() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        // Send / Search / Go fields want their action; everything else wants a newline.
        if (action != null &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            connection.performEditorAction(action)
        } else {
            connection.commitText("\n", 1)
        }
    }

    override fun switchToNextKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToNextInputMethod(false)
        } else {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
    }
}
