package com.devos.inputpasteparser

import java.io.File

sealed interface CapturedContent {

    data class Text(
        val value: String
    ) : CapturedContent

    data class Link(
        val url: String,
        val rawText: String
    ) : CapturedContent

    data class Image(
        val savedFile: File,
        val originalUri: String?
    ) : CapturedContent

    data class Video(
        val savedFile: File,
        val originalUri: String?
    ) : CapturedContent

    data class GenericFile(
        val savedFile: File,
        val originalFileName: String,
        val mimeType: String?,
        val originalUri: String?
    ) : CapturedContent

    data object Empty : CapturedContent
}