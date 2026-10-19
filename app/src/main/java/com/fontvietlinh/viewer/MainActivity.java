package com.fontvietlinh.viewer;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private TextView tvFontName;
    private TextView tvFontSizeLabel;
    private TextView tvPreview;
    private TextView tvGlyphsSample;
    private TextInputEditText etCustomText;
    private Slider sliderFontSize;
    private Button btnPickFont;

    private Typeface currentTypeface = Typeface.DEFAULT;

    private final ActivityResultLauncher<String[]> openFontLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    loadCustomFontFromUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFontName = findViewById(R.id.tvFontName);
        tvFontSizeLabel = findViewById(R.id.tvFontSizeLabel);
        tvPreview = findViewById(R.id.tvPreview);
        tvGlyphsSample = findViewById(R.id.tvGlyphsSample);
        etCustomText = findViewById(R.id.etCustomText);
        sliderFontSize = findViewById(R.id.sliderFontSize);
        btnPickFont = findViewById(R.id.btnPickFont);

        btnPickFont.setOnClickListener(v -> {
            openFontLauncher.launch(new String[]{
                    "font/*",
                    "application/x-font-ttf",
                    "application/x-font-opentype",
                    "application/octet-stream",
                    "*/*"
            });
        });

        sliderFontSize.addOnChangeListener((slider, value, fromUser) -> {
            int sizeSp = (int) value;
            tvFontSizeLabel.setText(getString(R.string.label_font_size, sizeSp));
            tvPreview.setTextSize(sizeSp);
        });

        etCustomText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null || s.length() == 0) {
                    tvPreview.setText(getString(R.string.sample_vietnamese_text));
                } else {
                    tvPreview.setText(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCustomFontFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Không thể đọc file font", Toast.LENGTH_SHORT).show();
                return;
            }

            File tempFont = new File(getCacheDir(), "custom_preview_font.ttf");
            FileOutputStream outputStream = new FileOutputStream(tempFont);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Typeface typeface = Typeface.createFromFile(tempFont);
            if (typeface != null) {
                currentTypeface = typeface;
                tvPreview.setTypeface(currentTypeface);
                tvGlyphsSample.setTypeface(currentTypeface);

                String displayName = uri.getLastPathSegment();
                if (displayName == null) displayName = "Font người dùng";
                tvFontName.setText("Font đã tải: " + displayName);
                Toast.makeText(this, "Đã áp dụng font thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Định dạng font không hỗ trợ", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi nạp font: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
