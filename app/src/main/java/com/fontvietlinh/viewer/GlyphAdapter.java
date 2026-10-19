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
    private int cellWidthDp = 78;
    private final int ink = Color.rgb(25, 39, 61);
    public GlyphAdapter(Context ctx, OnGlyphClick cb) { context = ctx; listener = cb; }
    public void show(List<FontCmapReader.Glyph> data, Typeface face) {
        items.clear(); items.addAll(data); font = face; notifyDataSetChanged();
    }
    /** Actual width of one grid column after responsive span calculation. */
    public void setCellWidthDp(int dp) {
        if (cellWidthDp == dp) return;
        cellWidthDp = dp;
        notifyDataSetChanged();
    }
    static String hex(int cp) { return String.format(java.util.Locale.US, "U+%04X", cp); }
    private int dp(int n) { return (int)(context.getResources().getDisplayMetrics().density*n + .5f); }
    @NonNull @Override public Tile onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(3),dp(4),dp(3),dp(4));
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
        int heightDp = Math.max(72, Math.min(276, Math.round(cellWidthDp * 1.12f)));
        ViewGroup.LayoutParams params = tile.itemView.getLayoutParams();
        if (params.height != dp(heightDp)) {
            params.height = dp(heightDp);
            tile.itemView.setLayoutParams(params);
        }
        LinearLayout.LayoutParams symbolParams = (LinearLayout.LayoutParams) tile.symbol.getLayoutParams();
        int symbolHeightDp = Math.round(heightDp * .65f);
        if (symbolParams.height != dp(symbolHeightDp)) {
            symbolParams.height = dp(symbolHeightDp);
            tile.symbol.setLayoutParams(symbolParams);
        }
        LinearLayout.LayoutParams codeParams = (LinearLayout.LayoutParams) tile.code.getLayoutParams();
        codeParams.height = dp(Math.max(16, Math.round(heightDp * .22f)));
        tile.code.setLayoutParams(codeParams);
        tile.symbol.setTextSize(Math.max(18, Math.min(92, cellWidthDp * .37f)));
        tile.code.setTextSize(Math.max(9, Math.min(15, cellWidthDp * .105f)));
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
