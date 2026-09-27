package com.devos.inputpasteparser.ui

import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.devos.inputpasteparser.CapturedContent
import java.io.File

/**
 * Exposes the current recording state plus [start]/[stop] triggers for
 * [SmartInputPasteItem]'s Record button.
 */
class AudioRecorderState internal constructor(
    val isRecording: Boolean,
    val start: () -> Unit,
    val stop: () -> Unit
)

/**
 * Remembers audio-recording state for [SmartInputPasteItem].
 *
 * Recording bypasses [com.devos.inputpasteparser.SmartReader.classifyUri]
 * entirely — the recorded file is already known to be audio, so a
 * [CapturedContent.Audio] is built directly and delivered via [onResult]
 * once recording stops.
 */
@Composable
fun rememberAudioRecorderState(
    onResult: (CapturedContent) -> Unit
): AudioRecorderState {

    val context = LocalContext.current

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    fun actuallyStartRecording() {
        try {
            val file = File(
                context.cacheDir,
                "recording_${System.currentTimeMillis()}.m4a"
            )

            val newRecorder = (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    ).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)

                    prepare()
                    start()
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
                onResult(
                    CapturedContent.Audio(
                        savedFile = file,
                        originalUri = null
                    )
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            actuallyStartRecording()
        }
    }

    fun start() {
        if (hasAudioRecordPermission(context)) {
            actuallyStartRecording()
        } else {
            permissionLauncher.launch(audioRecordPermission())
        }
    }

    return AudioRecorderState(
        isRecording = isRecording,
        start = { start() },
        stop = { stopRecording() }
    )
}