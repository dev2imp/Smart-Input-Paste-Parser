package com.devos.inputpasteparser.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devos.inputpasteparser.CapturedContent
import com.devos.inputpasteparser.SmartPasteReaderImpl
import com.devos.inputpasteparser.SmartReader
import kotlinx.coroutines.launch
import java.io.File

/**
 * A single, self-contained input item: type, paste, or pick a file —
 * the correct preview (text, link, image thumbnail, video/file icon)
 * renders automatically. Handles the runtime media-permission prompt
 * needed for clipboard image/video access internally, so callers never
 * have to think about it.
 */
@Composable
fun SmartInputPasteItem(
    modifier: Modifier = Modifier,
    reader: SmartReader? = null,
    onContentChanged: (CapturedContent?) -> Unit = {}
) {
    val context = LocalContext.current
    val actualReader = reader ?: remember { SmartPasteReaderImpl(context = context) }
    val scope = rememberCoroutineScope()

    var content by remember { mutableStateOf<CapturedContent?>(null) }
    var isTyping by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(content) {
        onContentChanged(content)
    }

    fun performClipboardRead() {
        isTyping = false
        scope.launch {
            content = actualReader.readClipboard()
        }
    }
    fun startRecording() {
        try {
            val file = File(
                context.cacheDir,
                "recording_${System.currentTimeMillis()}.m4a"
            )

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)

                    prepare()
                    start()
                }
            } else {
                TODO("VERSION.SDK_INT < S")
            }

            recorder = newRecorder
            recordingFile = file
            isRecording = true

        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            recordingFile = null
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }

            recordingFile?.let { file ->
                content = CapturedContent.Audio(
                    savedFile = file,
                    originalUri = null
                )
            }

        } catch (e: Exception) {
            recordingFile?.delete()
        } finally {
            recorder = null
            recordingFile = null
            isRecording = false
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            performClipboardRead()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isTyping = false
        scope.launch {
            content = actualReader.classifyUri(uri.toString())
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    isTyping = true
                    content = null
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Type")
                }

                IconButton(onClick = {
                    if (hasMediaReadPermission(context)) {
                        performClipboardRead()
                    } else {
                        permissionsLauncher.launch(mediaPermissionsFor(Build.VERSION.SDK_INT))
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                }

                IconButton(onClick = {
                    isTyping = false
                    filePickerLauncher.launch(arrayOf("*/*"))
                }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Select File")
                }
                IconButton(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            startRecording()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isRecording)
                            Icons.Default.Stop
                        else
                            Icons.Default.Mic,
                        contentDescription = if (isRecording)
                            "Stop recording"
                        else
                            "Record audio",
                        tint = if (isRecording)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (isTyping) {
                OutlinedTextField(
                    value = typedText,
                    onValueChange = {
                        typedText = it
                        content = if (it.isBlank()) null else CapturedContent.Text(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type something") }
                )
            } else {
                ContentPreview(content)
            }
        }
    }
}

private fun mediaPermissionsFor(sdkInt: Int): Array<String> {
    return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun hasMediaReadPermission(context: android.content.Context): Boolean {
    return mediaPermissionsFor(Build.VERSION.SDK_INT).all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun ContentPreview(content: CapturedContent?) {
    when (content) {
        null -> Text("Type, Paste or Choose File")
        is CapturedContent.Text -> Text(content.value)

        is CapturedContent.Link -> Text("🔗 ${content.url}")

        is CapturedContent.Image -> {
            val bitmap = remember(content.savedFile) {
                BitmapFactory.decodeFile(content.savedFile.absolutePath)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
            } else {
                Text("Couldn't Upload Image")
            }
        }
        is CapturedContent.Video -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, contentDescription = null)
                Text(" ${content.savedFile.name}")
            }
        }
        is CapturedContent.Audio -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null
                )
                Text(" ${content.savedFile.name}")
            }
        }

        is CapturedContent.GenericFile -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                Text(" ${content.originalFileName}")
            }
        }
        CapturedContent.Empty -> Text("Empty Clipboard")

    }

}