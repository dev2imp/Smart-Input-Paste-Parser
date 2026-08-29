
package com.devos.inputpasteparser.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
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
 * the correct preview renders automatically.
 */
@Composable
fun SmartInputPasteItem(
    modifier: Modifier = Modifier,
    reader: SmartReader? = null,
    onContentChanged: (CapturedContent?) -> Unit = {}
) {
    val context = LocalContext.current
    val actualReader = reader ?: remember {
        SmartPasteReaderImpl(context = context)
    }
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

            val newRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        isTyping = false

        scope.launch {
            content = actualReader.classifyUri(uri.toString())
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ---------------------------------------------------------
            // ACTION BUTTONS
            // ---------------------------------------------------------

            Text(
                text = "Add Content",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Edit,
                    label = "Type",
                    onClick = {
                        isTyping = true
                        content = null
                    }
                )

                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ContentPaste,
                    label = "Paste",
                    onClick = {
                        if (hasMediaReadPermission(context)) {
                            performClipboardRead()
                        } else {
                            permissionsLauncher.launch(
                                mediaPermissionsFor(Build.VERSION.SDK_INT)
                            )
                        }
                    }
                )

                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AttachFile,
                    label = "File",
                    onClick = {
                        isTyping = false
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }
                )

                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = if (isRecording) {
                        Icons.Default.Stop
                    } else {
                        Icons.Default.Mic
                    },
                    label = if (isRecording) "Stop" else "Record",
                    isActive = isRecording,
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            startRecording()
                        }
                    }
                )
            }

            // ---------------------------------------------------------
            // INPUT / PREVIEW
            // ---------------------------------------------------------

            if (isTyping) {

                OutlinedTextField(
                    value = typedText,
                    onValueChange = {
                        typedText = it

                        content =
                            if (it.isBlank()) {
                                null
                            } else {
                                CapturedContent.Text(it)
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = {
                        Text("Type something...")
                    },
                    minLines = 3
                )

            } else {

                PreviewContainer {
                    ContentPreview(content)
                }
            }
        }
    }
}


// =====================================================================
// ACTION BUTTON
// =====================================================================

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = 2.dp,
        onClick = onClick
    ) {

        Column(
            modifier = Modifier.padding(
                vertical = 10.dp,
                horizontal = 4.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isActive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}


// =====================================================================
// PREVIEW CONTAINER
// =====================================================================

@Composable
private fun PreviewContainer(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.45f
        ),
        tonalElevation = 1.dp
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {

            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            content()
        }
    }
}


// =====================================================================
// CONTENT PREVIEW
// =====================================================================

@Composable
private fun ContentPreview(
    content: CapturedContent?
) {

    when (content) {

        // -------------------------------------------------------------
        // EMPTY
        // -------------------------------------------------------------

        null -> {
            EmptyPreview()
        }


        // -------------------------------------------------------------
        // TEXT
        // -------------------------------------------------------------

        is CapturedContent.Text -> {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {

                Text(
                    text = content.value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }


        // -------------------------------------------------------------
        // LINK
        // -------------------------------------------------------------

        is CapturedContent.Link -> {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {

                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "🔗",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = content.url,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }


        // -------------------------------------------------------------
        // IMAGE
        // -------------------------------------------------------------

        is CapturedContent.Image -> {

            val bitmap = remember(content.savedFile) {
                BitmapFactory.decodeFile(
                    content.savedFile.absolutePath
                )
            }

            if (bitmap != null) {

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(150.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                )

            } else {

                Text(
                    text = "Couldn't load image",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }


        // -------------------------------------------------------------
        // VIDEO
        // -------------------------------------------------------------

        is CapturedContent.Video -> {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {

                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Video",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {

                        Text(
                            text = "Video",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = content.savedFile.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }


        // -------------------------------------------------------------
        // AUDIO
        // -------------------------------------------------------------

        is CapturedContent.Audio -> {

            var isPlaying by remember {
                mutableStateOf(false)
            }

            var mediaPlayer by remember {
                mutableStateOf<MediaPlayer?>(null)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // PLAY / STOP BUTTON

                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPlaying) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        onClick = {

                            if (isPlaying) {

                                mediaPlayer?.stop()
                                mediaPlayer?.release()

                                mediaPlayer = null
                                isPlaying = false

                            } else {

                                mediaPlayer = MediaPlayer().apply {

                                    setDataSource(
                                        content.savedFile.absolutePath
                                    )

                                    setOnPreparedListener {
                                        it.start()
                                        isPlaying = true
                                    }

                                    setOnCompletionListener {

                                        isPlaying = false

                                        it.release()

                                        mediaPlayer = null
                                    }

                                    prepareAsync()
                                }
                            }
                        }
                    ) {

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = if (isPlaying) {
                                    Icons.Default.Stop
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (isPlaying) {
                                    "Stop"
                                } else {
                                    "Play"
                                },
                                tint = if (isPlaying) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }


                    // AUDIO INFORMATION

                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {

                        Text(
                            text = "Audio Recording",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = content.savedFile.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )

                        Text(
                            text = if (isPlaying) {
                                "Playing..."
                            } else {
                                "Tap play to listen"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }


        // -------------------------------------------------------------
        // GENERIC FILE
        // -------------------------------------------------------------

        is CapturedContent.GenericFile -> {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {

                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {

                        Text(
                            text = "File",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = content.originalFileName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }


        // -------------------------------------------------------------
        // EMPTY CLIPBOARD
        // -------------------------------------------------------------

        CapturedContent.Empty -> {
            EmptyPreview()
        }
    }
}


// =====================================================================
// EMPTY PREVIEW
// =====================================================================

@Composable
private fun EmptyPreview() {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )

        Text(
            text = "No content yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        Text(
            text = "Type, paste or choose a file",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}


// =====================================================================
// MEDIA PERMISSIONS
// =====================================================================

private fun mediaPermissionsFor(
    sdkInt: Int
): Array<String> {

    return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {

        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )

    } else {

        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
}


// =====================================================================
// CHECK MEDIA PERMISSION
// =====================================================================

private fun hasMediaReadPermission(
    context: android.content.Context
): Boolean {

    return mediaPermissionsFor(Build.VERSION.SDK_INT)
        .all { permission ->

            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
}
