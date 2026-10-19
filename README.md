# VietLinh Font Viewer 2.1 — Android / GitHub Actions

**Nâng cấp từ bản 2.0**, giao diện sáng tối giản, hai tab: **XEM CHỮ** và **GLYPH / UNICODE**.

## Tính năng mới

- **English** (mặc định) — mẫu thử: `Every letter tells a story.\nThe quick brown fox jumps over the lazy dog.`. Đây là câu thử font có đủ 26 ký tự Latin.
- **Tiếng Việt không dấu** — mẫu thử giữ nguyên nội dung: `Me ngoi nguyen cau hang bao dem, loi kinh vong xa that em dem`.
- Chạm một trong hai lựa chọn dạng chip ở mục **Mẫu chữ** để thay văn bản mẫu. Vẫn có thể tự gõ nội dung khác.
- **Zoom hai ngón tay ở XEM CHỮ**: chụm để giảm, mở rộng để tăng kích thước chữ; thanh trượt đồng bộ. Giới hạn **12–160 sp**. Một ngón để cuộn, chạm để nhập chữ như bình thường.
- **Zoom hai ngón tay ở GLYPH / UNICODE**: tăng giảm đồng thời kích thước glyph, nhãn mã Unicode và chiều cao ô. Số cột trong lưới thay đổi tự động theo chiều rộng màn hình (1–12 cột), không bóp méo đường nét font.
- Zoom trên lưới sử dụng cùng một cỡ ô khi chuyển qua **Tất cả / Tiếng Việt / Latin / Số / dấu**. Có thể nhấn một ô để mở ký tự lớn, Unicode và glyph ID, rồi sao chép ký tự.

## Tính năng giữ nguyên

- Mở **TTF / OTF**, chạy offline; đọc danh sách Unicode qua bảng `cmap` của font thật, không dùng một danh sách ký tự cố định để giả lập glyph.
- Tra cứu theo ký tự, `U+XXXX`, hoặc mã Unicode; lọc và đếm kết quả.
- Đọc `maxp.numGlyphs` để hiển thị tổng glyph riêng với số codepoint được ánh xạ.

## Giới hạn

- Lưới Unicode chỉ hiển thị glyph có ánh xạ trong `cmap`. ALT/LIGA **không có Unicode riêng** không được liệt kê độc lập (nhưng vẫn nằm trong tổng số glyph); để duyệt độc lập tất cả Glyph ID cần đọc thêm outline + GSUB.
- Hỗ trợ TTF/OTF đơn lẻ, chưa xử lý TTC/OTC, WOFF, font nhiều trục. Cmap: format 0, 4 và 12. Giới hạn quét 130.000 ánh xạ.
- Không có môi trường Android SDK trong gói nguồn, vì vậy cần xác nhận biên dịch bằng GitHub Actions như bản trước.

## Cách cập nhật qua GitHub

1. Giải nén `VietLinhFontViewer_v2_1_GitHub.zip` trên Mac.
2. Trong repository `Fontvietlinh/Viewfont`, chọn **Code → Add file → Upload files**.
3. Tải các file và thư mục **bên trong** thư mục mới giải nén lên gốc repo, ghi đè file cũ khi GitHub cho phép, giữ nguyên đường dẫn của các thư mục con.
4. Commit vào nhánh `main`, mở tab **Actions** → chờ workflow **Build VietLinh Font Viewer APK** chạy và tải artifact `VietLinhFontViewer-APK`.
5. Cài `app-debug.apk` từ artifact trên Android. Nếu bị lỗi chữ ký debug không khớp, gỡ bản debug cũ trước khi cài.

Workflow giữ nguyên `android-actions/setup-android@v4`, Gradle 8.4, JDK 17, Ubuntu 24.04. Task: `gradle :app:assembleDebug`. Debug package: `com.fontvietlinh.viewer.debug`.
