# VietLinh Font Viewer - Android project ready for GitHub Actions

Ung dung Android de xem font OTF/TTF, nhap van ban tieng Viet de xem truoc, thay doi co chu va xem bo ky tu co dau.

## Cach tao APK tren GitHub (khong can cai Android Studio)

1. Giai nen file `VietLinhFontViewer_GitHub_READY.zip` tren may Mac.
2. Mo kho GitHub **https://github.com/Fontvietlinh/Viewfont**.
3. Bam **uploading an existing file** (neu kho con rong) hoac **Add file > Upload files**.
4. Mo thu muc da giai nen. **Can tai cac tep va thu muc BEN TRONG**, khong tai nguyen file zip va khong tai thu muc me lam mot cap thu muc long nhau.
5. Vi `.github` la thu muc an tren macOS, trong Finder nhan **Command + Shift + .** de hien thi truoc khi chon tat ca tep.
6. Keo tha tat ca thanh phan goc vao trang Upload files: `.github`, `app`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `README.md`, `.gitignore`.
7. O cuoi trang, bam **Commit changes** de tai file len nhanh `main`.
8. Chon tab **Actions**. Workflow **Build VietLinh Font Viewer APK** se duoc kich hoat khi push len `main`; hoac chon ten workflow > **Run workflow > Run workflow**.
9. Khi workflow hien dau tick mau xanh, mo trang lan chay, tim muc **Artifacts** > **VietLinhFontViewer-APK**.
10. Tai tep zip artifact ve, giai nen lay **app-debug.apk** de chuyen sang Android va cai.

**Chu y:** Android cho phep cai `app-debug.apk` ma khong can Play Store; neu dien thoai can, cap quyen *Allow from this source* cho ung dung quan ly tep. Day la ban debug danh cho thu nghiem, khong phai ban release da ky bang khoa rieng de dang Play Store.

## Cau truc GitHub dung

```
Viewfont/
  .github/workflows/build-apk.yml
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/java/com/fontvietlinh/viewer/MainActivity.java
  app/src/main/res/... (layout, values, mipmap launcher)
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  README.md
```

## Ghi chu build

- GitHub Actions su dung JDK 17, Android SDK 34 va Gradle 8.4, khong can Gradle Wrapper.
- Android Gradle Plugin 8.3.0; minSdk 24; targetSdk 34.
- Lenh build: `gradle --no-daemon :app:assembleDebug --stacktrace`.
- Dau ra: `app/build/outputs/apk/debug/app-debug.apk`.
- Moi lan push ma nguon len nhanh `main` hoac `master` se build lai.
- Neu Actions bi vo hieu hoa, vao repository **Settings > Actions > General** de bat Actions cho repo.

## Luu y ve kha nang kiem thu

Du an da duoc sua cac tai nguyen bi thieu va cau hinh build tren GitHub, nhung chua chay bien dich tren may cuc bo vi khong co Android SDK/Gradle tai moi truong chuan bi file. Hay su dung Actions de xac nhan build thuc te.
