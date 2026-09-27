package com.devos.inputpasteparser.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.devos.inputpasteparser.CapturedContent
import com.devos.inputpasteparser.SmartReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Remembers a clipboard-paste action for [SmartInputPasteItem].
 *
 * Returns a trigger function — call it (e.g. from a button's onClick) to
 * read the clipboard. If the clipboard holds media that requires a runtime
 * permission (see [mediaPermissionsFor]), the permission is requested first
 * and the read happens automatically once granted.
 */
@Composable
fun rememberClipboardPasteAction(
    reader: SmartReader,
    scope: CoroutineScope,
    onResult: (CapturedContent) -> Unit
): () -> Unit {

    val context = LocalContext.current

    fun performClipboardRead() {
        scope.launch {
            val result = reader.readClipboard()
            onResult(result)
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            performClipboardRead()
        }
    }

    return remember(permissionsLauncher) {
        {
            if (hasMediaReadPermission(context)) {
                performClipboardRead()
            } else {
                permissionsLauncher.launch(
                    mediaPermissionsFor()
                )
            }
        }
    }
}