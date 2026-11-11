package com.l6322059.bootimggetter;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 10;
    private static final int SAVE_BOOT = 11;
    private TextView status;
    private byte[] bootImage;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 48, 28, 28);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(0xFF10131B);

        TextView title = new TextView(this);
        title.setText("BOOT.IMG GETTER");
        title.setTextSize(26);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Extract boot.img from a firmware ZIP");
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFFB9C2D0);
        subtitle.setPadding(0, 16, 0, 28);
        layout.addView(subtitle);

        Button choose = new Button(this);
        choose.setText("SELECT FIRMWARE ZIP");
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("application/zip");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, PICK_ZIP);
        });
        layout.addView(choose);

        status = new TextView(this);
        status.setText("Ready. Select a firmware ZIP file.");
        status.setTextColor(0xFFFFFFFF);
        status.setTextSize(15);
        status.setPadding(0, 24, 0, 0);
        layout.addView(status);
        setContentView(layout);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == PICK_ZIP) {
            status.setText("Scanning archive…");
            new Thread(() -> scanZip(uri)).start();
        } else if (requestCode == SAVE_BOOT) {
            saveBoot(uri);
        }
    }

    private void scanZip(Uri uri) {
        try (InputStream raw = getContentResolver().openInputStream(uri);
             ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().matches("(^|.*/)boot\\.img$")) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = zip.read(buffer)) != -1) out.write(buffer, 0, n);
                    bootImage = out.toByteArray();
                    runOnUiThread(() -> {
                        status.setText("Found boot.img (" + bootImage.length + " bytes). Choose where to save it.");
                        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        save.setType("application/octet-stream");
                        save.putExtra(Intent.EXTRA_TITLE, "boot.img");
                        startActivityForResult(save, SAVE_BOOT);
                    });
                    return;
                }
            }
            runOnUiThread(() -> status.setText("No standalone boot.img found. If this ZIP contains payload.bin, this app cannot extract it yet."));
        } catch (Exception e) {
            runOnUiThread(() -> status.setText("Could not read ZIP: " + e.getMessage()));
        }
    }

    private void saveBoot(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null || bootImage == null) throw new Exception("Output destination unavailable");
            out.write(bootImage);
            status.setText("Success! boot.img saved.");
            Toast.makeText(this, "boot.img saved", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            status.setText("Save failed: " + e.getMessage());
        }
    }
}
