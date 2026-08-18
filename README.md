# Smart Input Paste Parser

A small Android library that turns **any** clipboard content or picked file — text, a link, an image, a video, or any other file — into one clean, typed result. No more writing a separate form for every content type: paste or pick, and the library tells you exactly what you got.

## Why

Most apps that accept user-pasted content end up with a pile of `if`/`else` branches checking MIME types, requesting permissions, and copying files to safe storage — repeated in every project that needs it. This library does that once, well, and exposes a single result type.

## What it gives you

- **`CapturedContent`** — a sealed type: `Text`, `Link`, `Image`, `Video`, `GenericFile`, or `Empty`.
- **`SmartReader`** — reads the system clipboard or classifies any `Uri` (from a file picker, share sheet, etc.) into one of the types above.
- **`SmartInputPasteItem`** — a drop-in Jetpack Compose composable with type / paste / pick-file buttons and automatic preview (thumbnail for images, filename for files, etc.). Runtime media-permission prompts are handled internally.

## Installation

Not yet published as a versioned artifact. For now, clone this repo alongside your project and add it as a local module:

```kotlin
// settings.gradle.kts
include(":app")   // this repo's library module

// your app's build.gradle.kts
dependencies {
    implementation(project(":app"))
}
```

Once published via JitPack, installation will be:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

// your app's build.gradle.kts
dependencies {
    implementation("com.github.dev2imp:Smart-Input-Paste-Parser:<version>")
}
```

## Usage

### Compose (recommended)

```kotlin
SmartInputPasteItem(
    onContentChanged = { content ->
        when (content) {
            is CapturedContent.Text       -> saveAsText(content.value)
            is CapturedContent.Link       -> saveAsLink(content.url)
            is CapturedContent.Image      -> saveAsImage(content.savedFile)
            is CapturedContent.Video      -> saveAsVideo(content.savedFile)
            is CapturedContent.GenericFile -> saveAsFile(content.savedFile, content.originalFileName)
            CapturedContent.Empty, null   -> Unit
        }
    }
)
```

### Programmatic (no UI)

```kotlin
val reader: SmartReader = SmartPasteReaderImpl(context = applicationContext)

val fromClipboard = reader.readClipboard()
val fromPickedUri = reader.classifyUri(uri.toString())
```

## Required permissions

The library's manifest declares these; they merge automatically into any app that depends on it:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```

`SmartInputPasteItem` requests these at runtime automatically before reading clipboard images or videos. If you use `SmartReader` directly without the Compose UI, request them yourself before calling `readClipboard()`.

## How content is classified

- **Clipboard**: if the clip holds a `Uri`, it's classified by MIME type (image / video / other). Otherwise the clipboard text is checked — if the *entire* trimmed string matches a URL pattern, it's a `Link`; otherwise it's `Text`.
- **Picked files**: any `Uri` passed to `classifyUri()` is resolved the same way — image/video get copied into the app's private storage (`filesDir/smartpaste/`) with a generated filename; everything else becomes a `GenericFile` with its original display name preserved.

## Requirements

- Android minSdk 24+
- Kotlin, Jetpack Compose (only required for `SmartInputPasteItem`; the reader classes have no Compose dependency)

## License

Not yet decided — add a `LICENSE` file before making the repository public. MIT or Apache 2.0 are common choices for libraries like this.

## Status

Early stage. Core classification logic and the Compose UI (`SmartInputPasteItem`) are implemented and manually tested against a local demo app, including the runtime media-permission flow and graceful fallback to `Empty` when permission is denied (no crash). The library manifest carries only the permissions it needs — no leftover application resources. Not yet published as a versioned artifact.