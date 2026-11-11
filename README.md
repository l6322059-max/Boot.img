# Boot.img Getter

Android app and desktop Python utility for extracting a standalone `boot.img` from a firmware ZIP.

## Android APK
The Android app detects the phone's manufacturer and model, provides a one-tap web search for matching firmware, then lets you select the downloaded firmware ZIP. It finds a standalone `boot.img` anywhere inside the archive and saves it using Android's document picker. It does not modify or flash a device.

### Download the APK
1. Open the repository's **Actions** tab.
2. Open the latest successful **Build Android APK** run.
3. Under **Artifacts**, download **BootImgGetter-debug**.
4. Extract the ZIP artifact and install `app-debug.apk` on your Android phone. You may need to allow installation from your browser/file manager.

The APK is built automatically whenever code is pushed to `main`, or manually with **Actions → Build Android APK → Run workflow**.

## Limitations
- Extracts a standalone `boot.img` stored in the firmware ZIP.
- Firmware discovery opens a web search; it does not silently download firmware or guarantee an official package exists for every model.
- Check the exact model number and region, and use the phone maker's official support source whenever possible.
- Does not yet unpack `payload.bin` (common in some firmware packages).
- Does not read protected Android partitions directly from the phone.
- Does not flash, patch, unlock, or write device partitions.

## Desktop version
The original `boot_img_getter.py` remains available for desktop use. Phone/ADB mode requires an authorized connection and existing root access.

## License
MIT
