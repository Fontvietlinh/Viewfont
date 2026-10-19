package com.fontvietlinh.viewer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Reads Unicode cmap from a TrueType/OpenType sfnt. It does not synthesize characters. */
public final class FontCmapReader {
    public static final class Glyph {
        public final int codepoint;
        public final int glyphId;
        public Glyph(int cp, int gid) { this.codepoint = cp; this.glyphId = gid; }
    }
    public static final class Result {
        public final List<Glyph> glyphs;
        public final int totalGlyphs;
        public final boolean truncated;
        Result(List<Glyph> entries, int n, boolean t) {
            glyphs = entries; totalGlyphs = n; truncated = t;
        }
    }

    private static final int MAX_MAPPINGS = 130000;
    private static final int MAX_CMAP_BYTES = 20 * 1024 * 1024;
    private FontCmapReader() {}

    private static int u16(byte[] b, int o) {
        return ((b[o] & 255) << 8) | (b[o + 1] & 255);
    }
    private static long u32(byte[] b, int o) {
        return (((long) u16(b, o)) << 16) | u16(b, o + 2);
    }
    private static boolean fits(byte[] b, long offset, long size) {
        return offset >= 0 && size >= 0 && offset <= b.length && size <= b.length - offset;
    }
    private static boolean validCp(long cp) {
        return cp > 0 && cp <= 0x10FFFF && (cp < 0xD800 || cp > 0xDFFF);
    }

    public static Result read(File file) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
            if (f.length() < 12) throw new IOException("Tệp quá nhỏ để là font TTF/OTF.");
            int header = f.readInt();
            if (header == 0x74746366) throw new IOException("Bản này hỗ trợ TTF/OTF, chưa đọc bộ TTC.");
            if (header != 0x00010000 && header != 0x4F54544F && header != 0x74727565)
                throw new IOException("Tệp không phải font TTF/OTF OpenType hợp lệ.");
            int tableCount = f.readUnsignedShort();
            f.skipBytes(6);
            if (tableCount < 1 || tableCount > 2048) throw new IOException("Bảng font không hợp lệ.");
            long cmapOffset = -1, cmapLength = -1, maxpOffset = -1, maxpLength = -1;
            for (int i = 0; i < tableCount; i++) {
                int tag = f.readInt();
                f.skipBytes(4); // checksum
                long offset = Integer.toUnsignedLong(f.readInt());
                long length = Integer.toUnsignedLong(f.readInt());
                if (offset > f.length() || length > f.length() - offset)
                    throw new IOException("Dữ liệu bảng font vượt kích thước tệp.");
                if (tag == 0x636D6170) { cmapOffset = offset; cmapLength = length; }
                if (tag == 0x6D617870) { maxpOffset = offset; maxpLength = length; }
            }
            if (cmapOffset < 0 || cmapLength < 4 || cmapLength > MAX_CMAP_BYTES)
                throw new IOException("Không tìm thấy bảng Unicode cmap hợp lệ.");
            int totalGlyphs = -1;
            if (maxpOffset >= 0 && maxpLength >= 6) {
                f.seek(maxpOffset + 4);
                totalGlyphs = f.readUnsignedShort();
            }
            byte[] cmap = new byte[(int)cmapLength];
            f.seek(cmapOffset);
            f.readFully(cmap);
            int recordCount = u16(cmap, 2);
            if (!fits(cmap, 4, 8L * recordCount)) throw new IOException("Bảng cmap bị lỗi.");
            Map<Integer, Integer> subtables = new HashMap<>();
            for (int i = 0; i < recordCount; i++) {
                int pos = 4 + 8 * i;
                int platform = u16(cmap, pos);
                int encoding = u16(cmap, pos + 2);
                long sub = u32(cmap, pos + 4);
                if (!fits(cmap, sub, 2)) continue;
                int format = u16(cmap, (int)sub);
                if ((platform == 0 || (platform == 3 && (encoding == 1 || encoding == 10)))
                        && (format == 4 || format == 12 || format == 0)) {
                    subtables.put((int)sub, format);
                }
            }
            TreeMap<Integer,Integer> entries = new TreeMap<>();
            boolean[] truncated = new boolean[]{false};
            // Process BMP tables then format 12: prefer the full repertoire when both exist.
            for (Map.Entry<Integer,Integer> table : subtables.entrySet()) {
                if (table.getValue() == 0) parse0(cmap, table.getKey(), entries, truncated);
                if (table.getValue() == 4) parse4(cmap, table.getKey(), entries, truncated);
            }
            for (Map.Entry<Integer,Integer> table : subtables.entrySet()) {
                if (table.getValue() == 12) parse12(cmap, table.getKey(), entries, truncated);
            }
            List<Glyph> list = new ArrayList<>(entries.size());
            for (Map.Entry<Integer,Integer> e : entries.entrySet()) list.add(new Glyph(e.getKey(), e.getValue()));
            return new Result(list, totalGlyphs, truncated[0]);
        }
    }

    private static void add(TreeMap<Integer,Integer> out, long cp, long gid, boolean[] truncated) {
        if (!validCp(cp) || gid <= 0 || gid > 65535) return;
        if (out.size() >= MAX_MAPPINGS && !out.containsKey((int)cp)) { truncated[0] = true; return; }
        out.put((int)cp, (int)gid);
    }

    private static void parse0(byte[] b, int p, TreeMap<Integer,Integer> out, boolean[] truncated) {
        if (!fits(b, p, 262) || u16(b,p + 2) < 262) return;
        for (int cp = 0; cp < 256; cp++) add(out, cp, b[p+6+cp] & 255, truncated);
    }

    private static void parse4(byte[] b, int p, TreeMap<Integer,Integer> out, boolean[] truncated) {
        if (!fits(b, p, 16)) return;
        int length = u16(b, p + 2), n = u16(b, p + 6) / 2;
        if (n == 0 || !fits(b, p, length) || length < 16L + 8L * n) return;
        int endBase = p + 14, startBase = p + 16 + 2*n;
        int deltaBase = p + 16 + 4*n, rangeBase = p + 16 + 6*n;
        for (int i = 0; i < n; i++) {
            int start = u16(b, startBase + 2*i), end = u16(b, endBase + 2*i);
            int delta = u16(b, deltaBase + 2*i);
            int range = u16(b, rangeBase + 2*i);
            if (start > end) continue;
            for (int cp = start; cp <= end; cp++) {
                int gid;
                if (range == 0) gid = (cp + delta) & 0xFFFF;
                else {
                    long indexPos = (long)rangeBase + 2L*i + range + 2L*(cp-start);
                    if (!fits(b, indexPos, 2) || indexPos + 2 > (long)p + length) continue;
                    gid = u16(b, (int)indexPos);
                    if (gid != 0) gid = (gid + delta) & 0xFFFF;
                }
                add(out, cp, gid, truncated);
            }
        }
    }

    private static void parse12(byte[] b, int p, TreeMap<Integer,Integer> out, boolean[] truncated) {
        if (!fits(b, p, 16)) return;
        long len = u32(b, p + 4), groups = u32(b, p + 12);
        if (!fits(b, p, len) || len < 16 || groups > (len - 16) / 12) return;
        for (int i = 0; i < groups; i++) {
            int q = p + 16 + i*12;
            long start = u32(b,q), end = u32(b,q+4), gid = u32(b,q+8);
            if (start > end || start > 0x10FFFF) continue;
            for (long cp = start; cp <= Math.min(end, 0x10FFFF); cp++) {
                add(out,cp,gid + cp - start,truncated);
                if (out.size() >= MAX_MAPPINGS) { truncated[0] = true; return; }
            }
        }
    }
}
