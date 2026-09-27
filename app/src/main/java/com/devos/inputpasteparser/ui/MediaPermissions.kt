package com.devos.inputpasteparser.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// =====================================================================
// MEDIA PERMISSIONS
// =====================================================================

/**
 * Returns the runtime permissions required to read media (images, video,
 * audio) from the device, based on the SDK version.
 *
 * - API 33+ (Tiramisu): granular per-type media permissions.
 * - Below API 33: legacy READ_EXTERNAL_STORAGE.
 */
fun mediaPermissionsFor(
    sdkInt: Int = Build.VERSION.SDK_INT
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

/**
 * Checks whether all media-read permissions (per [mediaPermissionsFor])
 * are currently granted.
 */
fun hasMediaReadPermission(
    context: Context
): Boolean {

    return mediaPermissionsFor(Build.VERSION.SDK_INT)
        .all { permission ->

            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
}

/**
 * Returns the runtime permission required to record audio.
 */
fun audioRecordPermission(): String =
    Manifest.permission.RECORD_AUDIO

/**
 * Checks whether the RECORD_AUDIO permission is currently granted.
 */
fun hasAudioRecordPermission(
    context: Context
): Boolean {

    return ContextCompat.checkSelfPermission(
        context,
        audioRecordPermission()
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Returns the runtime permission required to use the camera.
 */
fun cameraPermission(): String =
    Manifest.permission.CAMERA

/**
 * Checks whether the CAMERA permission is currently granted.
 */
fun hasCameraPermission(
    context: Context
): Boolean {

    return ContextCompat.checkSelfPermission(
        context,
        cameraPermission()
    ) == PackageManager.PERMISSION_GRANTED
}