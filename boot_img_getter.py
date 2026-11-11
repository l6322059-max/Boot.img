#!/usr/bin/env python3
"""Boot.img Getter: read-only firmware ZIP and ADB boot partition extractor."""
from __future__ import annotations
import shutil
import subprocess
import threading
import zipfile
from pathlib import Path
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

class BootImgGetter(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Boot.img Getter")
        self.geometry("720x520")
        self.minsize(620, 460)
        self.configure(bg="#10131c")
        self.output_dir = tk.StringVar(value=str(Path.home() / "BootImgGetter"))
        self.partition = tk.StringVar(value="/dev/block/by-name/boot")
        self._build()

    def _build(self):
        style = ttk.Style(self)
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass
        style.configure("TFrame", background="#10131c")
        style.configure("TLabel", background="#10131c", foreground="#e8ecf5")
        style.configure("Header.TLabel", font=("Segoe UI", 22, "bold"), foreground="#ffffff")
        style.configure("Sub.TLabel", foreground="#aab4c8")
        style.configure("TButton", padding=9)
        style.configure("TEntry", fieldbackground="#1c2230", foreground="#ffffff")
        outer = ttk.Frame(self, padding=22)
        outer.pack(fill="both", expand=True)
        ttk.Label(outer, text="BOOT.IMG GETTER", style="Header.TLabel").pack(anchor="w")
        ttk.Label(outer, text="Read-only extraction: Firmware ZIP + Android ADB", style="Sub.TLabel").pack(anchor="w", pady=(3, 18))
        out = ttk.LabelFrame(outer, text=" Output folder ", padding=10)
        out.pack(fill="x", pady=(0, 14))
        ttk.Entry(out, textvariable=self.output_dir).pack(side="left", fill="x", expand=True)
        ttk.Button(out, text="Browse", command=self.choose_output).pack(side="left", padx=(8, 0))
        modes = ttk.Notebook(outer)
        modes.pack(fill="both", expand=True)
        zip_tab = ttk.Frame(modes, padding=16)
        adb_tab = ttk.Frame(modes, padding=16)
        modes.add(zip_tab, text="Firmware ZIP")
        modes.add(adb_tab, text="Android phone (ADB)")
        ttk.Label(zip_tab, text="Extract an existing boot.img from a firmware ZIP.", style="Sub.TLabel").pack(anchor="w", pady=(0, 16))
        ttk.Button(zip_tab, text="Choose firmware ZIP and extract boot.img", command=self.choose_zip).pack(anchor="w")
        ttk.Label(adb_tab, text="Connect your own phone with USB debugging enabled.", style="Sub.TLabel").pack(anchor="w", pady=(0, 10))
        ttk.Button(adb_tab, text="Check device", command=self.check_device).pack(anchor="w")
        ttk.Label(adb_tab, text="Exact boot partition path (do not guess):").pack(anchor="w", pady=(16, 5))
        ttk.Entry(adb_tab, textvariable=self.partition).pack(fill="x")
        ttk.Label(adb_tab, text="Requires authorized ADB and existing root access.", style="Sub.TLabel").pack(anchor="w", pady=(5, 12))
        ttk.Button(adb_tab, text="Read boot partition to boot.img", command=self.read_partition).pack(anchor="w")
        self.status = tk.StringVar(value="Ready. Choose an extraction method.")
        ttk.Label(outer, textvariable=self.status, wraplength=660).pack(anchor="w", pady=(14, 0))
        ttk.Label(outer, text="SAFE MODE: reads only. Never flashes or writes partitions.", style="Sub.TLabel").pack(anchor="w", pady=(8, 0))

    def choose_output(self):
        folder = filedialog.askdirectory()
        if folder:
            self.output_dir.set(folder)

    def set_status(self, msg):
        self.after(0, lambda: self.status.set(msg))

    def ensure_output(self):
        path = Path(self.output_dir.get()).expanduser()
        path.mkdir(parents=True, exist_ok=True)
        return path

    def choose_zip(self):
        filename = filedialog.askopenfilename(filetypes=[("ZIP firmware", "*.zip"), ("All files", "*.*")])
        if filename:
            threading.Thread(target=self.extract_zip, args=(Path(filename),), daemon=True).start()

    def extract_zip(self, archive_path):
        try:
            out = self.ensure_output()
            found = False
            payload_seen = False
            with zipfile.ZipFile(archive_path) as zf:
                for info in zf.infolist():
                    if info.is_dir():
                        continue
                    base = Path(info.filename).name.lower()
                    if base == "boot.img":
                        with zf.open(info) as src, open(out / "boot.img", "wb") as dst:
                            shutil.copyfileobj(src, dst)
                        found = True
                        break
                    if base == "payload.bin":
                        payload_seen = True
            if found:
                self.set_status(f"Saved {out / 'boot.img'} ({(out / 'boot.img').stat().st_size:,} bytes).")
            elif payload_seen:
                self.set_status("No standalone boot.img found. This archive contains payload.bin; payload extraction is not included yet.")
            else:
                self.set_status("No boot.img found in this ZIP.")
        except (OSError, zipfile.BadZipFile) as exc:
            self.set_status(f"ZIP error: {exc}")

    def adb(self, args, timeout=30):
        return subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout)

    def check_device(self):
        def work():
            if not shutil.which("adb"):
                self.set_status("ADB not found. Install Android Platform Tools and add adb to PATH.")
                return
            try:
                result = self.adb(["devices"])
                lines = [x for x in result.stdout.splitlines()[1:] if x.strip()]
                ready = [x for x in lines if x.endswith("\tdevice")]
                if ready:
                    self.set_status("Authorized device: " + ready[0].split()[0])
                elif any("unauthorized" in x for x in lines):
                    self.set_status("Phone is unauthorized. Unlock it and accept the USB debugging prompt.")
                else:
                    self.set_status("No authorized device found. Check cable and USB debugging.")
            except (OSError, subprocess.TimeoutExpired) as exc:
                self.set_status(f"ADB check failed: {exc}")
        threading.Thread(target=work, daemon=True).start()

    def read_partition(self):
        part = self.partition.get().strip()
        if not part.startswith("/dev/block/") or any(c in part for c in (";", "&", "|", "$", "\n", "\r", "'", '"')):
            messagebox.showerror("Invalid path", "Enter a confirmed path beginning with /dev/block/.")
            return
        if not messagebox.askyesno("Confirm read-only extraction", f"Read this partition from your connected, rooted device?\n\n{part}\n\nNo device data will be changed."):
            return
        threading.Thread(target=self._read_partition_worker, args=(part,), daemon=True).start()

    def _read_partition_worker(self, part):
        if not shutil.which("adb"):
            self.set_status("ADB not found. Install Android Platform Tools and add adb to PATH.")
            return
        try:
            devices = self.adb(["devices"])
            ready = [x for x in devices.stdout.splitlines()[1:] if x.strip().endswith("\tdevice")]
            if not ready:
                self.set_status("No authorized ADB device. Connect and authorize your phone first.")
                return
            root_check = self.adb(["shell", "su", "-c", "id"], timeout=20)
            if root_check.returncode != 0 or "uid=0" not in root_check.stdout:
                self.set_status("Root read access unavailable. This tool cannot bypass device security.")
                return
            out = self.ensure_output() / "boot.img"
            proc = subprocess.Popen(["adb", "exec-out", "su", "-c", f"cat '{part}'"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            total = 0
            with open(out, "wb") as dst:
                while True:
                    chunk = proc.stdout.read(1024 * 1024)
                    if not chunk:
                        break
                    dst.write(chunk)
                    total += len(chunk)
            stderr = proc.stderr.read().decode("utf-8", "replace") if proc.stderr else ""
            code = proc.wait(timeout=120)
            if code != 0 or total == 0:
                out.unlink(missing_ok=True)
                self.set_status("Read failed. Verify partition path and root permission. " + stderr[:250])
            else:
                self.set_status(f"Saved {out} ({total:,} bytes).")
        except (OSError, subprocess.TimeoutExpired) as exc:
            self.set_status(f"ADB extraction failed: {exc}")

if __name__ == "__main__":
    BootImgGetter().mainloop()
