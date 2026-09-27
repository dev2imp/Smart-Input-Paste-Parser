package com.devos.inputpasteparser.ui


/**
 * Controls which capture methods [SmartInputPasteItem] shows to the user.
 *
 * All options default to `true` so existing callers keep full functionality
 * unless they explicitly opt out of a capture method.
 */
data class SmartInputConfig(
    val allowType: Boolean = true,
    val allowPaste: Boolean = true,
    val allowFile: Boolean = true,
    val allowCameraPhoto: Boolean = true,
    val allowCameraVideo: Boolean = true,
    val allowAudioRecord: Boolean = true
)