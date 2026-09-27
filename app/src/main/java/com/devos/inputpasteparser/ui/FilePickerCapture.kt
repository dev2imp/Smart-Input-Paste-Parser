package com.devos.inputpasteparser.ui


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.devos.inputpasteparser.CapturedContent
import com.devos.inputpasteparser.SmartReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Remembers a file-picker action for [SmartInputPasteItem].
 *
 * Returns a trigger function — call it (e.g. from a button's onClick) to
 * launch the system document picker. The picked [Uri] is classified via
 * [SmartReader.classifyUri], and the result is delivered through [onResult].
 *
 * No runtime permission is required here: [ActivityResultContracts.OpenDocument]
 * grants the app a scoped, temporary read URI regardless of storage permissions.
 */
@Composable
fun rememberFilePickerAction(
    reader: SmartReader,
    scope: CoroutineScope,
    onResult: (CapturedContent) -> Unit
): () -> Unit {

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = reader.classifyUri(uri.toString())
            onResult(result)
        }
    }

    return remember(filePickerLauncher) {
        {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }
}