Video to Frames - Android App
Fast and Offline Video Frame Extractor for Android - Built with Kotlin

A lightweight, fast, and 100 percent offline Android application that extracts frames/images from any video. Convert your videos to JPG/PNG frames in seconds.

Features
Fast Extraction - Extract hundreds of frames in seconds
100% Offline - No internet required, all processing on-device
Multiple Formats - Export as JPG / PNG
High Quality - Keep original video resolution
Easy to Use - Simple and clean UI
Lightweight - No heavy dependencies
Open Source - Free to use and modify
Screenshots
<!-- Add your screenshots here -->

Main Screen	Extracting	Result
<img src="screenshots/1.jpg" width="200"/>	<img src="screenshots/2.jpg" width="200"/>	<img src="screenshots/3.jpg" width="200"/>
How It Works
Select a video from your gallery
Choose frame interval / FPS
Select output format (JPG/PNG) and quality
Click Extract
Frames saved to /Pictures/VideoToFrames/
Tech Stack
Language: Kotlin
Platform: Android (Min SDK 21+)
Video Processing: MediaMetadataRetriever / FFmpeg
Architecture: MVVM
Installation
Clone the repository:
git clone https://github.com/hamzamazouz45-eng/F...
Open in Android Studio
Sync Gradle and Run
Or download APK from Releases: https://github.com/hamzamazouz45-eng/F.../releases

Usage Code Example
val retriever = MediaMetadataRetriever()
retriever.setDataSource(videoPath)
// Extract frame at 1 second (1_000_000 microseconds)
val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST)
saveBitmap(bitmap, "frame_001.jpg")
Keywords
android video to frames, video frame extractor, kotlin video processing, extract images from video, android mediametadataretriever, video to jpg

Contributing
Contributions are welcome! Feel free to:

Fork the project
Create your feature branch (git checkout -b feature/AmazingFeature)
Commit your changes (git commit -m 'Add some AmazingFeature')
Push to the branch (git push origin feature/AmazingFeature)
Open a Pull Request
License
Distributed under the MIT License. See LICENSE for more information.

Author
hamzamazouz45-eng

GitHub: https://github.com/hamzamazouz45-eng
If this project helped you, please give it a star!
