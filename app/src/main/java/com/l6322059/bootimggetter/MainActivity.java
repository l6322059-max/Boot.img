package com.l6322059.bootimggetter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 10;
    private static final int SAVE_BOOT = 11;
    private TextView status;
    private byte[] bootImage;

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8, 9, 13));
        getWindow().setNavigationBarColor(Color.rgb(8, 9, 13));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 34, 24, 24);
        root.setBackgroundColor(Color.rgb(8, 9, 13));

        TextView eyebrow = text("UTILITY  /  ANDROID", 11, 0xFF3B82F6, true);
        eyebrow.setLetterSpacing(0.16f);
        root.addView(eyebrow);

        TextView title = text("BOOT.IMG\nGETTER", 34, Color.WHITE, true);
        title.setLineSpacing(0, 0.94f);
        title.setPadding(0, 12, 0, 4);
        root.addView(title);

        TextView subtitle = text("Detect your phone and find matching firmware ZIPs, then extract boot.img.", 15, 0xFFB8C2D1, false);
        subtitle.setPadding(0, 8, 0, 26);
        root.addView(subtitle);

        String manufacturer = Build.MANUFACTURER == null ? "Android" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "Unknown model" : Build.MODEL;
        String deviceName = manufacturer + " " + model;
        TextView deviceLabel = text("DETECTED DEVICE", 11, 0xFF94A3B8, true);
        deviceLabel.setLetterSpacing(0.12f);
        root.addView(deviceLabel);
        TextView deviceInfo = text(deviceName + "\\nDevice code: " + Build.DEVICE, 16, Color.WHITE, true);
        deviceInfo.setPadding(0, 8, 0, 12);
        root.addView(deviceInfo);

        Button findFirmware = new Button(this);
        findFirmware.setText("FIND MATCHING FIRMWARE  ↗");
        findFirmware.setAllCaps(false);
        findFirmware.setTextSize(14);
        findFirmware.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        findFirmware.setTextColor(Color.WHITE);
        findFirmware.setBackground(shape(0xFF273449, 16));
        findFirmware.setPadding(10, 12, 10, 12);
        root.addView(findFirmware, new LinearLayout.LayoutParams(-1, -2));
        findFirmware.setOnClickListener(v -> {
            String query = "official firmware download " + manufacturer + " " + model + " " + Build.DEVICE;
            Intent search = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
            startActivity(search);
        });

        TextView hint = text("Searches for firmware matching this exact device. Check model and region before downloading.", 12, 0xFF94A3B8, false);
        hint.setPadding(0, 8, 0, 18);
        root.addView(hint);

        LinearLayout accent = new LinearLayout(this);
        accent.setOrientation(LinearLayout.HORIZONTAL);
        View cyan = new View(this);
        cyan.setBackground(shape(0xFF3B82F6, 8));
        accent.addView(cyan, new LinearLayout.LayoutParams(0, 4, 1));
        View pink = new View(this);
        pink.setBackground(shape(0xFF94A3B8, 8));
        LinearLayout.LayoutParams pinkParams = new LinearLayout.LayoutParams(0, 4, 1);
        pinkParams.leftMargin = 8;
        accent.addView(pink, pinkParams);
        root.addView(accent);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 22, 20, 22);
        card.setBackground(shape(0xFF171A23, 24));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = 28;
        root.addView(card, cardParams);

        TextView label = text("FIRMWARE EXTRACTOR", 12, 0xFF3B82F6, true);
        label.setLetterSpacing(0.12f);
        card.addView(label);

        TextView cardTitle = text("Get your boot.img", 22, Color.WHITE, true);
        cardTitle.setPadding(0, 10, 0, 6);
        card.addView(cardTitle);

        TextView info = text("After downloading firmware for your detected phone, select its ZIP. The app finds a standalone boot.img and lets you save it.", 14, 0xFFB8C2D1, false);
        info.setLineSpacing(5, 1f);
        card.addView(info);

        Button choose = new Button(this);
        choose.setText("SELECT DOWNLOADED FIRMWARE ZIP  →");
        choose.setAllCaps(false);
        choose.setTextSize(15);
        choose.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        choose.setTextColor(0xFF08090D);
        choose.setBackground(shape(0xFF3B82F6, 18));
        choose.setPadding(12, 16, 12, 16);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, -2);
        buttonParams.topMargin = 22;
        card.addView(choose, buttonParams);
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("application/zip");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, PICK_ZIP);
        });

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(18, 18, 18, 18);
        statusCard.setBackground(shape(0xFF11141C, 18));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = 18;
        root.addView(statusCard, statusParams);

        TextView statusLabel = text("STATUS", 11, 0xFF94A3B8, true);
        statusLabel.setLetterSpacing(0.14f);
        statusCard.addView(statusLabel);

        status = text("●  Ready — select a firmware ZIP to begin.", 14, Color.WHITE, false);
        status.setPadding(0, 10, 0, 0);
        status.setLineSpacing(4, 1f);
        statusCard.addView(status);

        TextView footer = text("SAFE MODE  •  READ-ONLY EXTRACTION", 10, 0xFF7F8DA3, true);
        footer.setGravity(Gravity.CENTER);
        footer.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = 26;
        root.addView(footer, footerParams);

        setContentView(root);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == PICK_ZIP) {
            status.setText("◌  Scanning firmware archive…");
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
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).matches("(^|.*/)boot\\.img$")) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = zip.read(buffer)) != -1) out.write(buffer, 0, n);
                    bootImage = out.toByteArray();
                    runOnUiThread(() -> {
                        status.setText("✓  Found boot.img (" + bootImage.length + " bytes). Choose where to save it.");
                        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        save.setType("application/octet-stream");
                        save.putExtra(Intent.EXTRA_TITLE, "boot.img");
                        startActivityForResult(save, SAVE_BOOT);
                    });
                    return;
                }
            }
            runOnUiThread(() -> status.setText("!  No standalone boot.img found. ZIP files containing payload.bin are not supported yet."));
        } catch (Exception e) {
            runOnUiThread(() -> status.setText("!  Could not read ZIP: " + e.getMessage()));
        }
    }

    private void saveBoot(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null || bootImage == null) throw new Exception("Output destination unavailable");
            out.write(bootImage);
            status.setText("✓  Success! boot.img saved.");
            Toast.makeText(this, "boot.img saved", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            status.setText("!  Save failed: " + e.getMessage());
        }
    }
}
