package com.fontvietlinh.viewer;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public final class GlyphAdapter extends RecyclerView.Adapter<GlyphAdapter.Tile> {
    public interface OnGlyphClick { void clicked(FontCmapReader.Glyph glyph); }
    private final Context context;
    private final OnGlyphClick listener;
    private final List<FontCmapReader.Glyph> items = new ArrayList<>();
    private Typeface font = Typeface.DEFAULT;
    private final int ink = Color.rgb(25, 39, 61);
    public GlyphAdapter(Context ctx, OnGlyphClick cb) { context = ctx; listener = cb; }
    public void show(List<FontCmapReader.Glyph> data, Typeface face) {
        items.clear(); items.addAll(data); font = face; notifyDataSetChanged();
    }
    static String hex(int cp) { return String.format(java.util.Locale.US, "U+%04X", cp); }
    private int dp(int n) { return (int)(context.getResources().getDisplayMetrics().density*n + .5f); }
    @NonNull @Override public Tile onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(3),dp(7),dp(3),dp(7));
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(94));
        params.setMargins(dp(3),dp(3),dp(3),dp(3)); cell.setLayoutParams(params);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(225,231,240)); cell.setBackground(background);
        TextView symbol = new TextView(context);
        symbol.setTextColor(ink); symbol.setGravity(Gravity.CENTER); symbol.setTextSize(30);
        symbol.setIncludeFontPadding(false); symbol.setSingleLine(true);
        cell.addView(symbol, new LinearLayout.LayoutParams(-1,dp(53)));
        TextView code = new TextView(context);
        code.setTextSize(10); code.setTextColor(Color.rgb(99,115,139)); code.setGravity(Gravity.CENTER);
        code.setSingleLine(true);
        cell.addView(code, new LinearLayout.LayoutParams(-1,dp(22)));
        return new Tile(cell,symbol,code);
    }
    @Override public void onBindViewHolder(@NonNull Tile tile, int pos) {
        FontCmapReader.Glyph glyph = items.get(pos);
        tile.symbol.setTypeface(font);
        tile.symbol.setText(new String(Character.toChars(glyph.codepoint)));
        tile.code.setText(hex(glyph.codepoint));
        tile.itemView.setOnClickListener(v -> listener.clicked(glyph));
    }
    @Override public int getItemCount() { return items.size(); }
    static final class Tile extends RecyclerView.ViewHolder {
        final TextView symbol, code;
        Tile(View root, TextView s, TextView c) { super(root); symbol=s; code=c; }
    }
}
