# MacBook Docker Desktop 開發流程

## 1. 這份文件要解決什麼

這份文件是給你在另一台 MacBook 上接手 `NUCOSMOS_POS` 專案時使用的。

重點觀念先講清楚：

- 專案不需要先出現在 Docker Desktop 裡
- 專案是從 GitHub `clone` 到你的 MacBook 本機資料夾
- Docker Desktop 只負責提供容器引擎，讓 PostgreSQL 容器可以跑起來
- 前端與後端仍然是在 Terminal 裡從專案資料夾啟動

一句話理解：

- `GitHub / 本機資料夾` 放的是專案程式碼
- `Docker Desktop` 放的是資料庫容器執行環境

## 2. MacBook 需要先準備什麼

請先確認你的 MacBook 已安裝：

- Git
- Node.js
- npm
- Java 17
- Docker Desktop

建議版本：

- Node.js 20 以上
- Java 17

## 3. 第一次在 MacBook 抓專案

先把專案抓到本機：

```bash
git clone https://github.com/stormcorn/NUCOSMOS_POS.git
cd NUCOSMOS_POS
```

如果專案之前已經抓過，只要更新：

```bash
git pull origin main
```

## 4. Docker Desktop 和專案的關係

這裡很重要。

你不需要在 Docker Desktop 介面裡「匯入專案」。

正確流程是：

1. 用 `git clone` 把專案抓到本機
2. 打開 Docker Desktop
3. 在 Terminal 進入專案的 `backend/`
4. 執行 `docker compose`
5. PostgreSQL 容器就會由 Docker Desktop 幫你跑起來

也就是說，Docker Desktop 不管你的專案檔案放哪裡，它只要負責跑容器就好。

## 5. 開啟 Docker Desktop

先手動打開 Docker Desktop。

你需要等到它進入可用狀態，再往下做。

可以用下面指令確認 Docker daemon 是否正常：

```bash
docker version
```

如果正常，你會看到 client 和 server 資訊。

你也可以再確認 compose：

```bash
docker compose version
```

## 6. 用 Docker Desktop 啟動 PostgreSQL

進入後端目錄：

```bash
cd backend
```

使用專案內的 compose 設定啟動 PostgreSQL：

```bash
docker compose --env-file .env.example up -d
```

啟動後確認容器：

```bash
docker compose ps
```

正常情況下你會看到 PostgreSQL 容器在運行，對外埠是：

- `localhost:5433`

## 7. 啟動 Spring Boot 後端

專案後端在 `backend/` 目錄下。

如果是第一次在 MacBook 上跑，先讓 Maven Wrapper 可執行：

```bash
chmod +x mvnw
```

啟動後端：

```bash
./mvnw spring-boot:run
```

後端預設網址：

- `http://localhost:8081`

健康檢查：

```bash
curl http://localhost:8081/api/v1/health
```

## 8. 啟動 Vue 管理後台前端

另開一個 Terminal，回到專案根目錄：

```bash
cd /你的路徑/NUCOSMOS_POS
```

安裝前端依賴：

```bash
npm install
```

啟動前端：

```bash
npm run dev
```

前端網址：

- `http://localhost:5173`

## 9. 目前本專案的本機埠

請記住目前標準配置：

- 前端：`http://localhost:5173`
- 後端：`http://localhost:8081`
- PostgreSQL：`localhost:5433`

## 10. 驗證流程

當三個服務都起來後，可以用下面順序檢查：

1. 前端首頁是否打得開
2. 後端健康檢查是否回 `UP`
3. PostgreSQL 容器是否 `Up`

建議指令：

```bash
curl http://localhost:8081/api/v1/health
docker compose -f backend/compose.yaml --env-file backend/.env.example ps
```

## 11. 管理員登入測試資料

目前可用測試資料：

- `storeCode`: `TW001`
- `roleCode`: `MANAGER`
- `pin`: `9999`
- `deviceCode`: `POS-TABLET-001`

## 12. 常見問題

### 12.1 Docker Desktop 已開，但 `docker version` 失敗

代表 Docker daemon 還沒真正 ready。

處理方式：

- 再等 10 到 30 秒
- 確認 Docker Desktop 視窗顯示 running
- 再重新執行 `docker version`

### 12.2 `docker compose up -d` 失敗

先確認你目前所在位置是：

```bash
cd backend
```

再確認 Docker Desktop 已經 ready。

### 12.3 `5433` 被占用

代表你本機已有其他服務使用這個埠。

先查誰占用：

```bash
lsof -i :5433
```

如果需要，我們再調整 `backend/.env.example` 和 compose 設定。

### 12.4 後端啟不起來

常見原因是 PostgreSQL 還沒起來。

先確認：

```bash
cd backend
docker compose ps
```

再確認：

```bash
curl http://localhost:8081/api/v1/health
```

### 12.5 前端啟不起來

先確認 Node 版本與依賴：

```bash
node -v
npm install
```

## 13. 最短版操作順序

如果你只想看最短流程，照這段做就可以：

```bash
git clone https://github.com/stormcorn/NUCOSMOS_POS.git
cd NUCOSMOS_POS
```

先手動打開 Docker Desktop，等它 ready。

```bash
cd backend
docker compose --env-file .env.example up -d
chmod +x mvnw
./mvnw spring-boot:run
```

另開一個 Terminal：

```bash
cd /你的路徑/NUCOSMOS_POS
npm install
npm run dev
```

最後打開：

- `http://localhost:5173`

