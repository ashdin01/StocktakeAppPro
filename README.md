# BackOfficePro Stocktake

Android app for scanning EAN-13 and EAN-8 barcodes to perform stock counts.
Works with [BackOfficePro](../BackOfficePro) over your local WiFi network.

## How it works

1. Run the API server on your BackOfficePro PC
2. Open the app on your Android phone (same WiFi)
3. Set the server URL in Settings
4. Create a stocktake session and start scanning

## Setup

### BackOfficePro API server

Install Flask into your BackOfficePro virtualenv:
```bash
cd ~/BackOfficePro
source venv/bin/activate
pip install flask
```

Start the API server:
```bash
./start_api.sh
# or: python api_server.py --port 5050
```

The server binds to all interfaces on port 5050. Find your PC's IP address with `ip addr` or `hostname -I`.

### Android app

**Sideload (easiest):**
Download `app-debug.apk` from the latest [GitHub Actions run](../../actions) or [Release](../../releases).
Enable "Install unknown apps" in your Android settings, then open the APK.

**Build from source:**
```bash
# Requires JDK 17 and Android SDK, or just open in Android Studio
gradle :app:assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

**F-Droid:**
This app has no proprietary dependencies and can be submitted to F-Droid.
See [CONTRIBUTING.md](CONTRIBUTING.md) for submission instructions.

## Requirements

- Android 7.0+ (API 24)
- Camera with autofocus
- Same WiFi network as the BackOfficePro PC

## Libraries

All open source (Apache 2.0):
- [ZXing](https://github.com/zxing/zxing) — barcode decoding
- [CameraX](https://developer.android.com/training/camerax) — camera
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — UI
- [Retrofit](https://square.github.io/retrofit/) — HTTP

## License

GPL-3.0 — see [LICENSE](LICENSE)
