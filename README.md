# Smart Input Paste Parser

A lightweight Android library that turns **clipboard content, picked files, camera captures, and recorded audio** into one clean, typed result.

Instead of handling text, links, images, videos, audio, files, MIME types, permissions, and file copying separately in every project, **Smart Input Paste Parser** provides a simple and consistent API for handling them.

## Features

* 📋 Clipboard content detection
* ✏️ Text input
* 🔗 Automatic URL detection
* 🖼️ Image detection and local storage
* 🎬 Video detection and local storage
* 🎵 Audio detection and playback
* 📷 Camera photo capture
* 🎥 Camera video capture
* 🎙️ Audio recording
* 📎 Generic file support
* 🧩 Typed `CapturedContent` result
* 🎨 Jetpack Compose UI, fully configurable per screen
* 🔐 Runtime permission handling (media, camera, microphone)
* 📦 JitPack distribution

---

# Installation

The library is available through **JitPack**.

### 1. Add JitPack

In your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. Add the dependency

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.dev2imp:Smart-Input-Paste-Parser:1.0.5")
}
```

Then sync your project.

---

# Usage

## Jetpack Compose

The easiest way to use the library is with `SmartInputPasteItem`.

```kotlin
SmartInputPasteItem(
    onContentChanged = { content ->
        when (content) {

            is CapturedContent.Text ->
                saveAsText(content.value)

            is CapturedContent.Link ->
                saveAsLink(content.url)

            is CapturedContent.Image ->
                saveAsImage(content.savedFile)

            is CapturedContent.Video ->
                saveAsVideo(content.savedFile)

            is CapturedContent.Audio ->
                saveAsAudio(content.savedFile)

            is CapturedContent.GenericFile ->
                saveAsFile(
                    content.savedFile,
                    content.originalFileName
                )

            CapturedContent.Empty, null ->
                Unit
        }
    }
)
```

By default, every capture method is enabled: Type, Paste, File, Photo, Video, and Record.

---

# SmartInputConfig

`SmartInputConfig` controls **which capture buttons are shown** to the user. Not every screen needs every capture method — a "leave a comment" field probably shouldn't offer a camera, while an "upload evidence" screen might want to hide typing entirely.

```kotlin
data class SmartInputConfig(
    val allowType: Boolean = true,
    val allowPaste: Boolean = true,
    val allowFile: Boolean = true,
    val allowCameraPhoto: Boolean = true,
    val allowCameraVideo: Boolean = true,
    val allowAudioRecord: Boolean = true
)
```

| Flag                | Controls                                             |
| -------------------- | ----------------------------------------------------- |
| `allowType`          | The "Type" button and inline text field               |
| `allowPaste`         | The "Paste" button (reads the system clipboard)        |
| `allowFile`          | The "File" button (opens the system document picker)  |
| `allowCameraPhoto`   | The "Photo" button (launches the camera for a still)   |
| `allowCameraVideo`   | The "Video" button (launches the camera for recording) |
| `allowAudioRecord`   | The "Record" button (records audio with the mic)       |

All flags default to `true`, so passing no config keeps the original full-featured behavior. Pass only the flags you want to turn off:

```kotlin
// Only allow pasting or picking a file — no typing, camera, or recording
SmartInputPasteItem(
    config = SmartInputConfig(
        allowType = false,
        allowCameraPhoto = false,
        allowCameraVideo = false,
        allowAudioRecord = false
    ),
    onContentChanged = { content -> /* ... */ }
)
```

Each flag only hides the *button*. The underlying capture logic (in `ClipboardCapture.kt`, `FilePickerCapture.kt`, `CameraCapture.kt`, `AudioRecorderCapture.kt`) is otherwise untouched — nothing is disabled at a lower level, so re-enabling a flag later requires no other changes.

---

# CapturedContent

Every capture path — no matter which button triggered it — resolves into one of these types. This is the entire point of the library: your app never needs to branch on *how* something was captured, only on *what* it turned out to be.

```kotlin
sealed interface CapturedContent {
    data class Text(val value: String) : CapturedContent
    data class Link(val url: String, val rawText: String) : CapturedContent
    data class Image(val savedFile: File, val originalUri: String?) : CapturedContent
    data class Video(val savedFile: File, val originalUri: String?) : CapturedContent
    data class Audio(val savedFile: File, val originalUri: String?) : CapturedContent
    data class GenericFile(
        val savedFile: File,
        val originalFileName: String,
        val mimeType: String?,
        val originalUri: String?
    ) : CapturedContent
    data object Empty : CapturedContent
}
```

| Type          | Meaning                                                                 |
| ------------- | ------------------------------------------------------------------------ |
| `Text`        | Plain typed or pasted text that isn't a URL                              |
| `Link`        | Text that matched a URL pattern; `rawText` keeps the original untrimmed string |
| `Image`       | Any image — from clipboard, file picker, **or** camera photo — copied locally |
| `Video`       | Any video — from clipboard, file picker, **or** camera video — copied locally |
| `Audio`       | Any audio — from clipboard, file picker, **or** a live recording          |
| `GenericFile` | Anything that isn't image/video/audio (PDFs, docs, archives, etc.)        |
| `Empty`       | Clipboard was empty, or no content has been captured yet                  |

Because `Image`/`Video`/`Audio` are reached from multiple capture methods, the *preview* rendering (`ContentPreview.kt`) doesn't need to know or care which button produced the content — an image looks the same whether it came from a paste or a camera shot.

`Audio` is the one exception worth knowing about internally: a live recording is built directly as `CapturedContent.Audio` without going through MIME-type classification, since the library already knows it just recorded an `.m4a` file. Every other type flows through the same URI classification path (`SmartReader.classifyUri`).

---

# Required Setup in the Consuming App

If you enable `allowCameraPhoto` or `allowCameraVideo` (either directly or by leaving the defaults), **your app** — not this library — needs its own `FileProvider` declared. This is a normal Android requirement: whichever app is asking the camera app to write into its private storage must expose a `FileProvider` so the camera can be handed a safe, temporary URI to write into.

This is **not automatic** and **not something the library can set up on your behalf**, since it depends on your app's own package name and manifest.

### 1. Add a `file_paths.xml` resource

`src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="camera" path="camera/" />
</paths>
```

This must match the folder the library writes camera captures into (`cacheDir/camera`).

### 2. Declare the provider in your manifest

Inside `<application>` in your `AndroidManifest.xml`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

`${applicationId}` resolves automatically to your app's own package name at build time — you don't need to hardcode it.

### 3. Declare the permissions

As siblings of `<application>`, not inside it:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

(Media read permissions for clipboard/file access are already declared by the library itself and don't need to be repeated.)

### Do I need this if I only use Paste / File / Type?

No. The `FileProvider` and `CAMERA`/`RECORD_AUDIO` permissions are only needed if `allowCameraPhoto`, `allowCameraVideo`, or `allowAudioRecord` are enabled (which they are by default). If your `SmartInputConfig` disables all three, you can skip this section entirely.

---

# Permissions Summary

| Permission                                             | Required for                          | Requested by       |
| -------------------------------------------------------- | -------------------------------------- | ------------------- |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (API 33+) | Reading media from the clipboard        | The library, at runtime |
| `READ_EXTERNAL_STORAGE` (below API 33)                    | Reading media from the clipboard        | The library, at runtime |
| `CAMERA`                                                 | Photo and video capture                | The library, at runtime — but **you** must declare it in your manifest |
| `RECORD_AUDIO`                                           | Audio recording                        | The library, at runtime — but **you** must declare it in your manifest |

The library requests these at runtime automatically when a relevant button is pressed. Declaring them in the manifest is your responsibility; requesting/handling the runtime prompt is the library's.

---

# Requirements

* Android `minSdk 24+`
* Kotlin
* AndroidX
* Jetpack Compose for `SmartInputPasteItem`

The core reader functionality (`SmartReader`) does not require Compose.

---

# License

License information will be added in a future release.