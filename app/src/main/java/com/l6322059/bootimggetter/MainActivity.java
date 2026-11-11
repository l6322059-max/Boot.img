package com.l6322059.bootimggetter;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
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

        TextView eyebrow = text("UTILITY  /  ANDROID", 11, 0xFF25F4EE, true);
        eyebrow.setLetterSpacing(0.16f);
        root.addView(eyebrow);

        TextView title = text("BOOT.IMG\nGETTER", 34, Color.WHITE, true);
        title.setLineSpacing(0, 0.94f);
        title.setPadding(0, 12, 0, 4);
        root.addView(title);

        TextView subtitle = text("Find and extract boot images from firmware ZIPs.", 15, 0xFFADB4C2, false);
        subtitle.setPadding(0, 8, 0, 26);
        root.addView(subtitle);

        LinearLayout accent = new LinearLayout(this);
        accent.setOrientation(LinearLayout.HORIZONTAL);
        View cyan = new View(this);
        cyan.setBackground(shape(0xFF25F4EE, 8));
        accent.addView(cyan, new LinearLayout.LayoutParams(0, 4, 1));
        View pink = new View(this);
        pink.setBackground(shape(0xFFFE2C55, 8));
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

        TextView label = text("FIRMWARE EXTRACTOR", 12, 0xFF25F4EE, true);
        label.setLetterSpacing(0.12f);
        card.addView(label);

        TextView cardTitle = text("Get your boot.img", 22, Color.WHITE, true);
        cardTitle.setPadding(0, 10, 0, 6);
        card.addView(cardTitle);

        TextView info = text("Choose a firmware ZIP. The app will look for a standalone boot.img and let you save it.", 14, 0xFFADB4C2, false);
        info.setLineSpacing(5, 1f);
        card.addView(info);

        Button choose = new Button(this);
        choose.setText("SELECT FIRMWARE ZIP  →");
        choose.setAllCaps(false);
        choose.setTextSize(15);
        choose.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        choose.setTextColor(0xFF08090D);
        choose.setBackground(shape(0xFF25F4EE, 18));
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

        TextView statusLabel = text("STATUS", 11, 0xFFFE2C55, true);
        statusLabel.setLetterSpacing(0.14f);
        statusCard.addView(statusLabel);

        status = text("●  Ready — select a firmware ZIP to begin.", 14, Color.WHITE, false);
        status.setPadding(0, 10, 0, 0);
        status.setLineSpacing(4, 1f);
        statusCard.addView(status);

        TextView footer = text("SAFE MODE  •  READ-ONLY EXTRACTION", 10, 0xFF737D8E, true);
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
