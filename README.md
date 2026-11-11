# Boot.img Getter

A read-only boot image extractor with two input modes:

- **Firmware ZIP:** finds and extracts a standalone boot.img from a firmware ZIP.
- **Android phone (ADB):** reads a boot partition from your own connected, already-rooted Android phone.

This tool never flashes, patches, unlocks, or writes to device partitions. Phone mode requires USB debugging, an authorized ADB connection, and existing root permission (su). Use only a partition path confirmed for your exact device.

## Requirements
- Python 3.10+
- Android Platform Tools (adb) for phone mode
- Existing root access on the phone for protected partition reads

## Run
```bash
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
python boot_img_getter.py
```

## Firmware ZIP
Choose Firmware ZIP, select your firmware archive, and choose an output folder. The app extracts an existing file named boot.img. If the package contains only payload.bin, this version reports that it cannot extract it yet.

## Android phone
1. Enable USB debugging on your own phone and connect it to your computer.
2. Accept the USB debugging authorization prompt.
3. Install Android Platform Tools and ensure adb is on PATH.
4. Select Android phone (ADB) and press Check device.
5. Enter the exact boot partition path confirmed for your device. Do not guess.
6. Press Read boot partition and approve the confirmation prompt.

If root access is unavailable, the app stops. It does not bypass Android security.

## Important
- Reads only; no flashing or partition writes.
- A/B devices may use slot-specific names such as boot_a and boot_b; verify the correct path from trusted device documentation.
- This is a desktop Python utility, not an Android APK. Android apps cannot directly read protected partitions without appropriate device privileges.
- Keep boot images private if they contain device-specific information.

## License
MIT
