# Flutter POS Android 平板測試 SOP

這份文件的目標是讓你在另一台有 Flutter SDK 的 MacBook 上，快速把 `pos-app/` 生成 Android 專案、安裝 APK，並在大陸製安卓平板上完成第一輪驗證。

## 1. 前置條件

請先確認：
- Git 已安裝
- Flutter SDK 已安裝
- Android Studio 或 Android SDK Command-line Tools 已安裝
- Docker Desktop 已啟動
- 本機後端與資料庫可正常啟動

建議先驗證：

```bash
flutter doctor
```

## 2. 取得專案

```bash
git clone https://github.com/stormcorn/NUCOSMOS_POS.git
cd NUCOSMOS_POS
```

## 3. 啟動後端服務

先啟 PostgreSQL：

```bash
cd backend
docker compose --env-file .env.example up -d
```

再啟 Spring Boot：

```bash
DB_PORT=5433 SERVER_PORT=8081 ./mvnw spring-boot:run
```

請確認健康檢查正常：

```bash
curl http://localhost:8081/api/v1/health
```

## 4. 生成 Flutter Android 專案

這個 repo 目前先存放的是 Flutter 來源碼骨架，`android/` 目錄請在有 Flutter SDK 的機器上生成。

```bash
cd ../pos-app
flutter create --platforms=android .
flutter pub get
```

## 5. 設定平板可連線的 API Base URL

實機平板不能用 `http://localhost:8081` 連到你的開發電腦，請改用該電腦的區網 IP。

先查出你 MacBook 的區網 IP，例如：

```bash
ipconfig getifaddr en0
```

假設得到 `192.168.1.111`，那 Flutter 啟動時請帶：

```bash
flutter run \
  --dart-define=POS_API_BASE_URL=http://192.168.1.111:8081 \
  --dart-define=POS_DEVICE_CODE=POS-TABLET-001
```

如果是輸出 APK：

```bash
flutter build apk \
  --release \
  --dart-define=POS_API_BASE_URL=http://192.168.1.111:8081 \
  --dart-define=POS_DEVICE_CODE=POS-TABLET-001
```

APK 產出位置通常是：

```text
pos-app/build/app/outputs/flutter-apk/app-release.apk
```

## 6. 第一輪測試帳號

管理員：
- `Store Code`: `TW001`
- `Role`: `MANAGER`
- `PIN`: `9999`
- `Device Code`: `POS-TABLET-001`

收銀員：
- `Store Code`: `TW001`
- `Role`: `CASHIER`
- `PIN`: `1234`
- `Device Code`: `POS-TABLET-001`

## 7. 第一輪實機驗證清單

請依序確認：
- App 可正常安裝到平板
- App 可正常啟動
- PIN 登入頁顯示正常
- 輸入門市、角色、PIN 後可成功登入
- 登入後可載入商品列表
- 活動商品會顯示活動價與原價
- 圖片可正常載入
- 平板橫向顯示比例正常
- 休眠喚醒後 App 仍可使用

## 8. 目前版本的範圍

這一版是 Flutter POS 第一輪驗證版，目前只聚焦：
- App 安裝
- PIN 登入
- JWT 會話恢復
- 商品清單
- 活動商品價格

目前還不包含：
- 購物車
- 結帳流程
- SQLite 離線資料庫
- 印表機
- 掃碼器
- 藍牙周邊
- kiosk 模式

## 9. 常見問題

### 平板登入失敗，但後端在本機可正常登入

通常是因為 Flutter 還在用 `localhost:8081`。  
請改成開發電腦的區網 IP，例如 `http://192.168.1.111:8081`。

### 平板可開 App，但商品清單是空的

請檢查：
- 是否已成功登入
- 後端 `http://<你的IP>:8081/api/v1/health` 是否可從同一網段設備打到
- 商品資料是否已存在於後端

### `flutter create` 後覆蓋了既有檔案嗎

這個目錄目前是刻意先放來源碼骨架，`flutter create --platforms=android .` 主要是補平台資料夾；執行前仍建議先確認 Git 工作樹乾淨，避免你不小心覆蓋未提交變更。
