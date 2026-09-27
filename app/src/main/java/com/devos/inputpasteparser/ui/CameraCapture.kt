package com.devos.inputpasteparser.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.devos.inputpasteparser.CapturedContent
import com.devos.inputpasteparser.SmartReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Creates a fresh output file under the app's cache dir and returns a
 * content:// [Uri] for it via [FileProvider], so the camera app can write
 * to it without needing broader storage access.
 *
 * Requires a matching <provider> entry in AndroidManifest.xml with
 * authority "${applicationId}.fileprovider".
 */
private fun createCameraOutputUri(context: Context, extension: String): Uri {
    val dir = File(context.cacheDir, "camera").apply {
        if (!exists()) mkdirs()
    }
    val file = File(dir, "${UUID.randomUUID()}.$extension")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

/**
 * Remembers a camera-photo action for [SmartInputPasteItem].
 *
 * Returns a trigger function — call it (e.g. from a button's onClick) to
 * request CAMERA permission if needed, then launch the camera to take a
 * photo. The result is classified via [SmartReader.classifyUri] (it will
 * resolve as [CapturedContent.Image]) and delivered through [onResult].
 */
@Composable
fun rememberCameraPhotoAction(
    reader: SmartReader,
    scope: CoroutineScope,
    onResult: (CapturedContent) -> Unit
): () -> Unit {

    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->

        val uri = pendingUri
        pendingUri = null

        if (success && uri != null) {
            scope.launch {
                val result = reader.classifyUri(uri.toString())
                onResult(result)
            }
        }
    }

    fun launchCamera() {
        val uri = createCameraOutputUri(context, "jpg")
        pendingUri = uri
        takePictureLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    return remember(takePictureLauncher, permissionLauncher) {
        {
            if (hasCameraPermission(context)) {
                launchCamera()
            } else {
                permissionLauncher.launch(cameraPermission())
            }
        }
    }
}

/**
 * Remembers a camera-video action for [SmartInputPasteItem].
 *
 * Returns a trigger function — call it (e.g. from a button's onClick) to
 * request CAMERA permission if needed, then launch the camera to record a
 * video. The result is classified via [SmartReader.classifyUri] (it will
 * resolve as [CapturedContent.Video]) and delivered through [onResult].
 */
@Composable
fun rememberCameraVideoAction(
    reader: SmartReader,
    scope: CoroutineScope,
    onResult: (CapturedContent) -> Unit
): () -> Unit {

    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->

        val uri = pendingUri
        pendingUri = null

        if (success && uri != null) {
            scope.launch {
                val result = reader.classifyUri(uri.toString())
                onResult(result)
            }
        }
    }

    fun launchCamera() {
        val uri = createCameraOutputUri(context, "mp4")
        pendingUri = uri
        captureVideoLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    return remember(captureVideoLauncher, permissionLauncher) {
        {
            if (hasCameraPermission(context)) {
                launchCamera()
            } else {
                permissionLauncher.launch(cameraPermission())
            }
        }
    }
}