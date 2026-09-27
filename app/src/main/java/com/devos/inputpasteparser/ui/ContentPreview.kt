package com.devos.inputpasteparser.ui

import android.graphics.BitmapFactory
import android.media.MediaPlayer
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.devos.inputpasteparser.CapturedContent

// =====================================================================
// PREVIEW CONTAINER
// =====================================================================

/**
 * Wraps a preview composable with the "Preview" label and shared styling.
 * Capture-method-agnostic — used regardless of how [content] was captured.
 */
@Composable
fun PreviewContainer(
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

/**
 * Renders the correct preview for [content], regardless of whether it came
 * from typing, pasting, the file picker, the camera, or audio recording.
 */
@Composable
fun ContentPreview(
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
        // IMAGE (from clipboard, file picker, or camera photo)
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
        // VIDEO (from clipboard, file picker, or camera video)
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
        // AUDIO (from clipboard, file picker, or recording)
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