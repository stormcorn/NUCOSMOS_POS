# MacBook 接手工作指南

## 1. 目的

這份文件是給你換到 MacBook 後快速接手 `NUCOSMOS_POS` 專案使用，目標是讓你可以：

- 從 GitHub 下載最新專案
- 啟動目前 repo 內可執行的前端原型
- 啟動 Spring Boot 後端
- 啟動 PostgreSQL 開發環境
- 驗證目前 PIN + JWT 登入流程

## 2. 目前專案內容

產品規劃方向是：

- POS 前端：Flutter
- 管理後台：Vue Web

但目前這個 repo 實際已包含的是：

- React + Vite 的前端原型
- Spring Boot 後端
- PostgreSQL + Flyway migration
- POS PIN + JWT 登入第一版
- PRD 與後端設計文件

## 3. 建議安裝環境

MacBook 建議先準備：

- Git
- Node.js
- npm
- Java 17+
- Docker Desktop

### 建議版本

- Node.js：20+ 或接近目前開發機版本
- Java：17+

目前這台開發機實際使用版本：

- Node.js：`v24.14.0`
- Java：`17+`

## 4. 從 GitHub 下載專案

```bash
git clone https://github.com/stormcorn/NUCOSMOS_POS.git
cd NUCOSMOS_POS
```

如果你之後只是要同步最新進度：

```bash
git pull origin main
```

## 5. 目前 repo 內前端原型啟動方式

### 5.1 安裝依賴

目前 repo 根目錄是 Node.js 前端原型，已包含 `package-lock.json`，所以直接執行：

```bash
npm install
```

### 5.2 啟動前端

```bash
npm run dev
```

預設原型前端位置：

- `http://localhost:5173`

## 6. 後端啟動方式

### 6.1 進入後端目錄

```bash
cd backend
```

### 6.2 啟動 PostgreSQL

這份設定預設使用 `5433`，避免和本機其他專案常見的 PostgreSQL `5432` 衝突。

```bash
docker compose --env-file .env.example up -d
```

### 6.3 啟動 Spring Boot

這個 repo 已經加入 Maven Wrapper，MacBook 上建議優先用它，不用依賴你本機有沒有裝 Maven。

第一次如果沒有執行權限，先跑：

```bash
chmod +x mvnw
```

啟動指令：

```bash
./mvnw spring-boot:run
```

預設後端位置：

- `http://localhost:8080`

如果你本機已有其他服務佔用 `8080`，可以改用 `8081`：

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

如果你同時也要指定這份 handoff 使用的 PostgreSQL `5433`，可直接執行：

```bash
DB_PORT=5433 SERVER_PORT=8081 ./mvnw spring-boot:run
```

## 7. 驗證目前後端功能

### 7.1 健康檢查

```bash
curl http://localhost:8080/api/v1/health
```

如果後端是用 `8081` 啟動，請改成：

```bash
curl http://localhost:8081/api/v1/health
```

### 7.2 PIN 登入

```bash
curl -X POST http://localhost:8080/api/v1/auth/pin-login \
  -H "Content-Type: application/json" \
  -d '{
    "storeCode": "TW001",
    "roleCode": "CASHIER",
    "pin": "1234",
    "deviceCode": "POS-TABLET-001"
  }'
```

如果後端是用 `8081` 啟動，請把網址改成 `http://localhost:8081`。

### 7.3 取得目前登入者

先把登入回傳的 `accessToken` 存起來，再呼叫：

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <your-token>"
```

如果後端是用 `8081` 啟動，請把網址改成 `http://localhost:8081`。

### 7.4 取得商品列表

```bash
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <your-token>"
```

如果後端是用 `8081` 啟動，請把網址改成 `http://localhost:8081`。

## 8. 目前可用測試帳號

### 門市

- `TW001`

### 裝置

- `POS-TABLET-001`

### PIN 測試資料

- `CASHIER` + `1234`
- `MANAGER` + `9999`
- `CASHIER` 或 `MANAGER` + `5678`

## 9. 重要文件

- [POS 系統實用架構 PRD](./pos-system-prd.md)
- [後端基礎建設文件](./backend-foundation.md)
- [PIN + JWT 登入設計](./pin-jwt-auth-design.md)

## 10. 建議工作順序

你在 MacBook 接手後，建議先照這個順序做：

1. `git clone`
2. 前端原型 `npm install`
3. 後端 `cd backend`
4. `docker compose --env-file .env.example up -d`
5. `./mvnw test`
6. `./mvnw spring-boot:run`
7. 再回 repo 根目錄啟動前端原型 `npm run dev`

## 11. 如果遇到問題

### 11.1 `./mvnw: Permission denied`

```bash
chmod +x backend/mvnw
```

### 11.2 Docker 沒起來

確認 Docker Desktop 已開啟，然後重跑：

```bash
cd backend
docker compose --env-file .env.example up -d
```

如果看到 `Bind for 0.0.0.0:5432 failed: port is already allocated`，代表本機已有其他 PostgreSQL 在使用 `5432`。
這份 handoff 預設已改成 `5433`，請確認你是使用 repo 內目前的 [`backend/.env.example`](/Users/stormcorn/NUCOSMOS_POS/backend/.env.example)。

### 11.3 前端安裝失敗

先確認 Node 版本，再重跑：

```bash
node -v
npm install
```

### 11.4 後端連不到資料庫

先確認 PostgreSQL container 是否啟動：

```bash
cd backend
docker compose ps
```

### 11.5 後端 `8080` 已被占用

如果看到 `Port 8080 was already in use`，代表本機已有其他服務使用 `8080`。

可改用：

```bash
cd backend
DB_PORT=5433 SERVER_PORT=8081 ./mvnw spring-boot:run
```
