package com.fontvietlinh.viewer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.Gravity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private TextView tvFontName, tvFontInfo, tvFontSizeLabel, tvPreview, tvGlyphCount, tvGlyphEmpty;
    private EditText etCustomText, etGlyphSearch;
    private MaterialButton btnPickFont;
    private View previewPage, glyphPage;
    private TabLayout tabLayout;
    private GlyphAdapter glyphAdapter;
    private Typeface currentTypeface = Typeface.DEFAULT;
    private FontCmapReader.Result currentResult;
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private String category = "all";
    private boolean loading = false;

    private static final String VIETNAMESE =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
          + "ĐđĂăÂâÊêÔôƠơƯư"
          + "ÀÁẢÃẠàáảãạẰẮẲẴẶằắẳẵặẦẤẨẪẬầấẩẫậ"
          + "ÈÉẺẼẸèéẻẽẹỀẾỂỄỆềếểễệ"
          + "ÌÍỈĨỊìíỉĩị"
          + "ÒÓỎÕỌòóỏõọỒỐỔỖỘồốổỗộỜỚỞỠỢờớởỡợ"
          + "ÙÚỦŨỤùúủũụỪỨỬỮỰừứửữự"
          + "ỲÝỶỸỴỳýỷỹỵ";

    private final ActivityResultLauncher<String[]> pickFont =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) loadFont(uri);
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        tvFontName = findViewById(R.id.tvFontName);
        tvFontInfo = findViewById(R.id.tvFontInfo);
        tvFontSizeLabel = findViewById(R.id.tvFontSizeLabel);
        tvPreview = findViewById(R.id.tvPreview);
        tvGlyphCount = findViewById(R.id.tvGlyphCount);
        tvGlyphEmpty = findViewById(R.id.tvGlyphEmpty);
        etCustomText = findViewById(R.id.etCustomText);
        etGlyphSearch = findViewById(R.id.etGlyphSearch);
        btnPickFont = findViewById(R.id.btnPickFont);
        previewPage = findViewById(R.id.previewPage);
        glyphPage = findViewById(R.id.glyphPage);
        tabLayout = findViewById(R.id.tabLayout);

        RecyclerView grid = findViewById(R.id.glyphGrid);
        int span = getResources().getConfiguration().screenWidthDp >= 420 ? 5 : 4;
        grid.setLayoutManager(new GridLayoutManager(this, span));
        grid.setHasFixedSize(true);
        glyphAdapter = new GlyphAdapter(this, this::showGlyphDetails);
        grid.setAdapter(glyphAdapter);

        btnPickFont.setOnClickListener(v -> pickFont.launch(new String[]{"*/*"}));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                boolean preview = tab.getPosition() == 0;
                previewPage.setVisibility(preview ? View.VISIBLE : View.GONE);
                glyphPage.setVisibility(preview ? View.GONE : View.VISIBLE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        Slider sizeSlider = findViewById(R.id.sliderFontSize);
        sizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            tvFontSizeLabel.setText(String.format(Locale.US,"Cỡ chữ  %d sp",Math.round(value)));
            tvPreview.setTextSize(value);
        });
        etCustomText.addTextChangedListener(simpleWatcher(() -> {
            String input = etCustomText.getText().toString();
            tvPreview.setText(input.isEmpty() ? getString(R.string.sample_vietnamese_text) : input);
        }));
        etGlyphSearch.addTextChangedListener(simpleWatcher(this::filterGlyphs));
        ChipGroup chips = findViewById(R.id.glyphChips);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (id == R.id.chipVietnamese) category = "vi";
            else if (id == R.id.chipLatin) category = "latin";
            else if (id == R.id.chipSymbols) category = "symbols";
            else category = "all";
            filterGlyphs();
        });
    }

    private TextWatcher simpleWatcher(Runnable runnable) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { runnable.run(); }
            @Override public void afterTextChanged(Editable e) {}
        };
    }

    private String documentName(@NonNull Uri uri) {
        String name = "Font TTF/OTF";
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) name = cursor.getString(0);
        } catch (Exception ignored) {}
        return name;
    }

    private void loadFont(Uri uri) {
        if (loading) return;
        String filename = documentName(uri);
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".ttf") && !lower.endsWith(".otf")) {
            Toast.makeText(this,"Hãy chọn font có đuôi .ttf hoặc .otf",Toast.LENGTH_LONG).show();
            return;
        }
        loading = true;
        btnPickFont.setEnabled(false);
        tvFontInfo.setText("Đang đọc Unicode cmap…");
        reader.execute(() -> {
            try {
                File temp = new File(getCacheDir(), "font_vietlinh" + (lower.endsWith(".otf") ? ".otf" : ".ttf"));
                long total = 0;
                try (InputStream input = getContentResolver().openInputStream(uri);
                     FileOutputStream output = new FileOutputStream(temp)) {
                    if (input == null) throw new IllegalStateException("Không đọc được tệp font");
                    byte[] data = new byte[8192];
                    int n;
                    while ((n = input.read(data)) != -1) {
                        total += n;
                        if (total > 60L*1024*1024) throw new IllegalStateException("Font vượt quá 60 MB");
                        output.write(data,0,n);
                    }
                }
                FontCmapReader.Result found = FontCmapReader.read(temp);
                Typeface face = Typeface.createFromFile(temp);
                runOnUiThread(() -> {
                    loading = false; btnPickFont.setEnabled(true);
                    currentTypeface = face;
                    currentResult = found;
                    tvPreview.setTypeface(face);
                    tvFontName.setText(filename);
                    tvFontInfo.setText(found.glyphs.size() + " mã Unicode được ánh xạ"
                            + (found.totalGlyphs >= 0 ? " · " + found.totalGlyphs + " glyph trong font" : ""));
                    filterGlyphs();
                    Toast.makeText(this,"Đã nạp font! Chọn tab Glyph / Unicode để xem.",Toast.LENGTH_SHORT).show();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    loading = false; btnPickFont.setEnabled(true);
                    tvFontInfo.setText("Không đọc được font vừa chọn");
                    new AlertDialog.Builder(this).setTitle("Không thể mở font")
                            .setMessage(ex.getMessage() == null ? "Tệp không hỗ trợ" : ex.getMessage())
                            .setPositiveButton("Đóng",null).show();
                });
            }
        });
    }

    private boolean matchesCategory(int cp) {
        switch(category) {
            case "vi": return VIETNAMESE.indexOf(new String(Character.toChars(cp))) >= 0;
            case "latin": return (cp >= 0x20 && cp <= 0x024F)
                    || (cp >= 0x1E00 && cp <= 0x1EFF)
                    || (cp >= 0x2C60 && cp <= 0x2C7F);
            case "symbols": return Character.isDigit(cp)
                    || (!Character.isLetter(cp) && !Character.isISOControl(cp));
            default: return true;
        }
    }

    private void filterGlyphs() {
        if (currentResult == null) {
            glyphAdapter.show(new ArrayList<>(), currentTypeface);
            tvGlyphCount.setText("Chưa nạp font");
            tvGlyphEmpty.setVisibility(View.VISIBLE);
            tvGlyphEmpty.setText("Chọn file font .TTF hoặc .OTF để xem Unicode thực tế.");
            return;
        }
        String raw = etGlyphSearch.getText().toString().trim();
        String q = raw.toUpperCase(Locale.ROOT);
        String hexQuery = q.replace("U+", "").replace("0X", "");
        int cpExact = -1;
        if (!raw.isEmpty() && raw.codePointCount(0, raw.length()) == 1
                && !Character.isDigit(raw.codePointAt(0))) cpExact = raw.codePointAt(0);
        List<FontCmapReader.Glyph> found = new ArrayList<>();
        for (FontCmapReader.Glyph glyph : currentResult.glyphs) {
            int cp = glyph.codepoint;
            if (!matchesCategory(cp)) continue;
            if (cpExact >= 0) {
                if (cp != cpExact) continue;
            } else if (!q.isEmpty() && !String.format(Locale.US,"%04X",cp).contains(hexQuery)
                    && !Integer.toString(cp).equals(q)) continue;
            found.add(glyph);
        }
        glyphAdapter.show(found, currentTypeface);
        tvGlyphCount.setText(found.size() + " / " + currentResult.glyphs.size() + " Unicode"
                + (currentResult.truncated ? " · giới hạn quét" : ""));
        tvGlyphEmpty.setVisibility(found.isEmpty() ? View.VISIBLE : View.GONE);
        tvGlyphEmpty.setText("Không tìm thấy mã Unicode phù hợp.");
    }

    private int dp(int size) { return (int)(size*getResources().getDisplayMetrics().density + .5f); }
    private void showGlyphDetails(FontCmapReader.Glyph glyph) {
        String value = new String(Character.toChars(glyph.codepoint));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(22),dp(12),dp(22),dp(6));
        TextView preview = new TextView(this);
        preview.setText(value); preview.setTypeface(currentTypeface); preview.setTextSize(82);
        preview.setGravity(Gravity.CENTER);
        panel.addView(preview,new LinearLayout.LayoutParams(-1,dp(124)));
        TextView info = new TextView(this);
        info.setGravity(Gravity.CENTER); info.setTextSize(16);
        info.setText(GlyphAdapter.hex(glyph.codepoint) + "  ·  GID " + glyph.glyphId
                + "\nUnicode decimal: " + glyph.codepoint);
        panel.addView(info,new LinearLayout.LayoutParams(-1,dp(65)));
        new AlertDialog.Builder(this).setTitle("Thông tin glyph")
                .setView(panel)
                .setNegativeButton("Đóng",null)
                .setPositiveButton("Sao chép ký tự",(dialog, which) -> {
                    ClipboardManager manager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                    manager.setPrimaryClip(ClipData.newPlainText("Glyph",value));
                    Toast.makeText(this,"Đã sao chép ký tự",Toast.LENGTH_SHORT).show();
                }).show();
    }

    @Override protected void onDestroy() { reader.shutdownNow(); super.onDestroy(); }
}
