# فريمر · Easy Framing

Extracts one frame every N seconds from a video, optionally keeping only the bottom part
(where burned-in subtitles are), and can read the Arabic subtitles offline (OCR) into
`subtitles.srt` and `subtitles.txt`.
No ads, no internet, no permissions. Images are named by timestamp (`00h12m05s.jpg`).

## Build the APK from your phone (no PC)

1. Upload/push the contents of this folder to your GitHub repository (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab → *Build APK* → wait 5–10 minutes.
3. Open the finished run → **Artifacts** → download `FrameGrab-apk`, unzip, install the APK.

The build downloads the Arabic OCR data and the Tajawal font by itself, so you do not need to add them.
If the build fails, copy the red error text and send it to Claude.

Building on your own PC instead? Put `ara.traineddata` (from tesseract-ocr/tessdata_best) in
`app/src/main/assets/tessdata/` and `Tajawal-Regular/Medium/Bold.ttf` in `app/src/main/res/font/`
as `tajawal_regular.ttf`, `tajawal_medium.ttf`, `tajawal_bold.ttf`.

## Change the look

Everything visual is in `app/src/main/java/com/framegrab/app/Theme.kt`.

## Change the texts

`app/src/main/res/values/strings.xml` (English) and `values-ar/strings.xml` (Arabic).
