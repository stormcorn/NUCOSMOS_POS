# NUCOSMOS POS App

這個目錄是 Flutter 版 POS App 的來源碼骨架，目標是先完成 Android 平板可安裝、可登入、可抓商品資料的第一輪實機驗證。

目前包含：
- PIN 登入頁
- POS 首頁殼
- 商品列表
- 與現有 Spring Boot API 的最小串接

## 目前狀態

這台電腦目前沒有安裝 Flutter SDK，所以這裡先放的是可追蹤的 Flutter 來源碼與設定檔，`android/` 平台資料夾需要在有 Flutter SDK 的機器上生成。

建議在 MacBook 上進入本目錄後執行：

```bash
flutter doctor
flutter create --platforms=android .
flutter pub get
```

## API 連線

平板實機不能使用 `localhost:8081` 連到你電腦上的後端，請改成開發主機的區網 IP。

例如：

```bash
flutter run \
  --dart-define=POS_API_BASE_URL=http://192.168.1.111:8081 \
  --dart-define=POS_DEVICE_CODE=POS-TABLET-001
```

## 第一輪測試目標

- Android App 可正常安裝
- App 可開啟並進入 PIN 登入頁
- 可用既有測試帳號登入
- 可成功抓取商品列表
- 可正確顯示活動商品價格

更完整步驟請看：
- [Flutter POS Android 平板測試 SOP](../docs/flutter-pos-tablet-sop.md)
