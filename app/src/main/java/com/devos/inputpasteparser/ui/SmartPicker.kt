package com.devos.inputpasteparser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devos.inputpasteparser.CapturedContent
import com.devos.inputpasteparser.SmartPasteReaderImpl
import com.devos.inputpasteparser.SmartReader

/**
 * A single, self-contained input item: type, paste, pick a file, take a
 * photo/video, or record audio — the correct preview renders automatically.
 *
 * Which capture methods are shown is controlled by [config].
 */
@Composable
fun SmartInputPasteItem(
    modifier: Modifier = Modifier,
    reader: SmartReader? = null,
    config: SmartInputConfig = SmartInputConfig(),
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

    LaunchedEffect(content) {
        onContentChanged(content)
    }

    // -------------------------------------------------------------
    // CAPTURE ACTIONS (each delegates to its own file)
    // -------------------------------------------------------------

    val performClipboardRead = rememberClipboardPasteAction(
        reader = actualReader,
        scope = scope,
        onResult = { result ->
            isTyping = false
            content = result
        }
    )

    val performFilePick = rememberFilePickerAction(
        reader = actualReader,
        scope = scope,
        onResult = { result ->
            isTyping = false
            content = result
        }
    )

    val performCameraPhoto = rememberCameraPhotoAction(
        reader = actualReader,
        scope = scope,
        onResult = { result ->
            isTyping = false
            content = result
        }
    )

    val performCameraVideo = rememberCameraVideoAction(
        reader = actualReader,
        scope = scope,
        onResult = { result ->
            isTyping = false
            content = result
        }
    )

    val audioRecorder = rememberAudioRecorderState(
        onResult = { result ->
            isTyping = false
            content = result
        }
    )

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

                if (config.allowType) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Edit,
                        label = "Type",
                        onClick = {
                            isTyping = true
                            content = null
                        }
                    )
                }

                if (config.allowPaste) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ContentPaste,
                        label = "Paste",
                        onClick = performClipboardRead
                    )
                }

                if (config.allowFile) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AttachFile,
                        label = "File",
                        onClick = performFilePick
                    )
                }

                if (config.allowCameraPhoto) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CameraAlt,
                        label = "Photo",
                        onClick = performCameraPhoto
                    )
                }

                if (config.allowCameraVideo) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        onClick = performCameraVideo
                    )
                }

                if (config.allowAudioRecord) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = if (audioRecorder.isRecording) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Mic
                        },
                        label = if (audioRecorder.isRecording) "Stop" else "Record",
                        isActive = audioRecorder.isRecording,
                        onClick = {
                            if (audioRecorder.isRecording) {
                                audioRecorder.stop()
                            } else {
                                audioRecorder.start()
                            }
                        }
                    )
                }
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