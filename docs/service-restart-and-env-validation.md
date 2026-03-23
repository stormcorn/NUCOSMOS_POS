# 服務重啟與環境變數驗證規範

## 1. 目的

這份文件定義 `NUCOSMOS_POS` 開發過程中的固定操作規則，避免出現「程式碼已更新，但服務仍在跑舊版本」或「文件設定正確，但執行時吃到錯誤環境變數」的問題。

## 2. 固定規則

### 規則 1：只要更新影響執行中服務的內容，更新後必須直接重啟

以下變更完成後，必須重啟對應服務，不可只改檔不重啟：

- Spring Boot Controller / Service / Repository / Security / Config
- Flyway migration
- `application.yml`
- `backend/.env.example`
- Vue `src/api/*`
- Vue `src/router/*`
- Vue `src/stores/*`
- `src/api/http.ts`
- 前端 `.env.example`
- Docker compose 或資料庫連線設定

對應重啟原則：

- 改到後端程式、migration、後端環境變數：重啟 `Spring Boot`
- 改到前端 Vite 環境變數、API base URL、路由初始化：重啟 `Vue dev server`
- 改到 PostgreSQL container 或 compose 設定：重啟 `Docker / PostgreSQL`

### 規則 2：環境變數要先驗證，再啟動，再驗證結果

啟動服務前，必須確認關鍵環境變數與專案規格一致；啟動後，必須再用實際服務結果驗證。

目前專案標準值：

- 前端：`http://localhost:5173`
- 後端：`http://localhost:8081`
- PostgreSQL：`localhost:5433`

## 3. 啟動前檢查

### 3.1 前端

確認前端 API 目標值：

```bash
VITE_API_BASE_URL=http://localhost:8081
```

檢查位置：

- [/.env.example](c:\NUCOSMOS_POS\.env.example)
- [src/api/http.ts](c:\NUCOSMOS_POS\src\api\http.ts)

### 3.2 後端

確認後端預設連線：

```bash
DB_HOST=localhost
DB_PORT=5433
DB_NAME=nucosmos_pos
DB_USERNAME=nucosmos
DB_PASSWORD=nucosmos_dev_password
SERVER_PORT=8081
FRONTEND_ORIGIN=http://localhost:5173
```

檢查位置：

- [backend/.env.example](c:\NUCOSMOS_POS\backend\.env.example)
- [backend/src/main/resources/application.yml](c:\NUCOSMOS_POS\backend\src\main\resources\application.yml)

### 3.3 PostgreSQL

確認 Docker compose 對外埠仍為：

```bash
5433 -> 5432
```

檢查位置：

- [backend/compose.yaml](c:\NUCOSMOS_POS\backend\compose.yaml)

## 4. 標準重啟流程

### 4.1 PostgreSQL

```bash
cd backend
docker compose --env-file .env.example up -d
```

驗證：

```bash
docker compose ps
```

預期：

- PostgreSQL container 狀態為 `Up`
- 對外埠為 `5433`

### 4.2 Spring Boot

Windows：

```powershell
cd C:\NUCOSMOS_POS\backend
$env:DB_HOST="localhost"
$env:DB_PORT="5433"
$env:DB_NAME="nucosmos_pos"
$env:DB_USERNAME="nucosmos"
$env:DB_PASSWORD="nucosmos_dev_password"
$env:SERVER_PORT="8081"
$env:FRONTEND_ORIGIN="http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

macOS：

```bash
cd /path/to/NUCOSMOS_POS/backend
DB_HOST=localhost \
DB_PORT=5433 \
DB_NAME=nucosmos_pos \
DB_USERNAME=nucosmos \
DB_PASSWORD=nucosmos_dev_password \
SERVER_PORT=8081 \
FRONTEND_ORIGIN=http://localhost:5173 \
./mvnw spring-boot:run
```

驗證：

```bash
curl http://localhost:8081/api/v1/health
```

預期：

- 回傳 `status: UP`

### 4.3 Vue 管理後台

```bash
npm install
npm run dev
```

驗證：

- 開啟 `http://localhost:5173`
- 確認前端 API 指向 `http://localhost:8081`

## 5. 變更後最低驗證清單

每次更新並重啟後，至少驗證以下項目：

### 5.1 後端

- `GET http://localhost:8081/api/v1/health`
- 如果有新增 API，至少手動打一次對應端點
- 如果有新增 migration，確認 Spring Boot 啟動時 Flyway 沒有失敗

### 5.2 前端

- 頁面能正常開啟
- 若有改 API 對接頁面，刷新後確認沒有舊資料或 `Unexpected server error`
- 若有改登入或權限，重新登入一次驗證 session

### 5.3 資料庫

- PostgreSQL container 狀態正常
- 埠號仍為 `5433`

## 6. 特別注意

- 不可假設目前執行中的後端一定是最新版本，改完 Controller / Service 後一定要重啟。
- 不可假設 `.env.example` 的值會自動生效；實際啟動命令是否帶入正確環境變數，必須另外驗證。
- 若發生 `Unexpected server error`，要先確認：
  - 服務是否真的已重啟
  - API 路由是否已載入
  - 後端是否吃到正確 `DB_PORT=5433` 與 `SERVER_PORT=8081`

## 7. 本專案固定基準

除非之後文件明確更新，否則統一使用：

- 前端：`http://localhost:5173`
- 後端：`http://localhost:8081`
- PostgreSQL：`localhost:5433`
