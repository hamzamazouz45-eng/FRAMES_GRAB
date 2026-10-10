# Frame Grab / مستخرج الفريمات

Extracts one frame every N seconds from a video, optionally keeping only the bottom part
(where burned-in subtitles are). No ads, no internet, no permissions.
Files are named by timestamp (`00h12m05s.jpg`) so they stay in sync with the video.

## Build the APK from your phone (no PC)

1. Create a free GitHub account and a new **private** repository.
2. Upload the contents of this folder (keep the `.github/workflows/build.yml` path).
3. Open the **Actions** tab → *Build APK* → wait 5–10 minutes.
4. Open the finished run → **Artifacts** → download `FrameGrab-apk`, unzip, install the APK.

If the build fails, copy the red error text and send it to Claude.

## Change the look

Everything visual is in `app/src/main/java/com/framegrab/app/Theme.kt`.

## Change the texts

`app/src/main/res/values/strings.xml` (English) and `values-ar/strings.xml` (Arabic).
