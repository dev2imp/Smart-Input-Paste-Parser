package com.devos.inputpasteparser

interface SmartReader {
    suspend fun readClipboard(): CapturedContent
    suspend fun classifyUri(uri: String): CapturedContent
}
