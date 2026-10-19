# VietLinh Font Viewer 2.0 — Android / GitHub Actions

Ứng dụng kiểm tra font TTF/OTF, có tab xem nội dung và tab **Glyph / Unicode**.

## Chức năng
- **XEM CHỮ**: mở font, gõ nội dung tiếng Việt, chỉnh cỡ chữ.
- **GLYPH / UNICODE**: bảng các codepoint được font ánh xạ thật trong `cmap`, hiện mã `U+XXXX`, tìm kiếm, lọc Tiếng Việt/Latin/Số & dấu, nhấn để xem to, xem GID và sao chép ký tự.
- Hiển thị `maxp.numGlyphs` (tổng glyph trong font) riêng với số mã Unicode đã ánh xạ.
- Giao diện sáng, gọn, dùng offline, không yêu cầu cấp quyền đọc toàn bộ bộ nhớ máy.

## Giới hạn của bản này
- Lưới v2 chỉ hiển thị **glyph có Unicode trong cmap**. Glyph ALT, LIGA, swash không có codepoint không thể hiển thị bằng TextView thông thường; chúng vẫn nằm trong tổng số glyph `maxp`. Về sau có thể thêm trình xem Glyph ID độc lập và kiểm tra GSUB.
- Mở file TTF/OTF đơn lẻ; chưa hỗ trợ TTC/OTC, WOFF/WOFF2 và font biến thể nhiều trục.
- Cmap hỗ trợ định dạng 0, 4, 12. Lưới được giới hạn tối đa 130.000 ánh xạ để tránh quá tải điện thoại.
- Kiểm tra render ở Android không thay thế kiểm tra contour/font features trong FontLab.

## Cập nhật repository GitHub

1. Giải nén **VietLinhFontViewer_v2_GitHub.zip** trên Mac.
2. Mở repository **Fontvietlinh/Viewfont**, chọn **Code → Add file → Upload files**.
3. Tải **nội dung bên trong thư mục đã giải nén** (gồm `app`, `.github` và các file Gradle) vào gốc repository, chọn ghi đè các file cũ.
4. Commit vào nhánh `main` → tab **Actions** → chờ **Build VietLinh Font Viewer APK** thành công → tải artifact `VietLinhFontViewer-APK`.
5. Nếu Android báo không thể cập nhật vì chữ ký debug khác nhau, gỡ bản debug cũ trước khi cài bản mới.

Workflow dùng `android-actions/setup-android@v4`, Ubuntu 24.04, Gradle 8.4 và Java 17, không cần Android Studio trên Mac.

Build task: `gradle :app:assembleDebug`. Package debug: `com.fontvietlinh.viewer.debug`.
